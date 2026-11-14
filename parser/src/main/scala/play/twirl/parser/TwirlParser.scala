/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.twirl.parser

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag
import scala.util.parsing.input.OffsetPosition

/**
 * TwirlParser is a recursive descent parser for a modified grammar of the Play2 template language as loosely defined
 * [[https://www.playframework.com/documentation/latest/Home here]] and more rigorously defined by the original template
 * parser, `play.templates.ScalaTemplateCompiler.TemplateParser`. TwirlParser is meant to be a near drop in replacement
 * for `play.templates.ScalaTemplateCompiler.TemplateParser`.
 *
 * The original grammar, as reversed-engineered from `play.templates.ScalaTemplateCompiler.TemplateParser`, is defined
 * as follows:
 * {{{
 *   parser : comment? whitespace? ('@' parentheses+)? templateContent
 *   templateContent : (importExpression | localDef | template | mixed)*
 *   templateDeclaration : '@' identifier squareBrackets? parentheses*
 *   localDef : templateDeclaration (' ' | '\t')* '=' (' ' | '\t') scalaBlock
 *   template : templateDeclaration (' ' | '\t')* '=' (' ' | '\t') '{' templateContent '}'
 *   mixed : (comment | scalaBlockDisplayed | caseExpression | matchExpression | forExpression | safeExpression | plain | expression) | ('{' mixed* '}')
 *   scalaBlockDisplayed : scalaBlock
 *   scalaBlockChained : scalaBlock
 *   scalaBlock : '@' brackets
 *   importExpression : '@' 'import' .* '\r'? '\n'
 *   caseExpression : whitespace? 'case' .+ '=>' block whitespace?
 *   forExpression : '@' "for" parentheses block
 *   matchExpression : '@' (simpleExpr | complexExpr) whitespaceNoBreak 'match' block
 *   simpleExpr : methodCall expressionPart*
 *   complexExpr : parentheses
 *   safeExpression : '@' parentheses
 *   elseCall : whitespaceNoBreak "else" whitespaceNoBreak?
 *   chainedMethods : ('.' methodCall)+
 *   expressionPart : chainedMethods | block | (whitespaceNoBreak scalaBlockChained) | elseCall | parentheses
 *   expression : '@' methodCall expressionPart*
 *   methodCall : identifier squareBrackets? parentheses?
 *   blockArgs : [^'=>' '\n']* '=>'
 *   block : whitespaceNoBreak '{' blockArgs? mixed* '}'
 *   brackets : '{' (brackets | [^'}'])* '}'
 *   comment : '@*' [^'*@']* '*@'
 *   parentheses : '(' (parentheses | [^')'])* ')'
 *   squareBrackets : '[' (squareBrackets | [^']'])* ']'
 *   plain : ('@@' | ([^'@'] [^'{' '}']))+
 *   whitespaceNoBreak : [' ' '\t']+
 *   identifier : javaIdentStart javaIdentPart* // see java docs for what these two rules mean
 * }}}
 *
 * TwirlParser implements a slightly modified version of the above grammar that removes some back-tracking within the
 * 'mixed' non-terminal. It is defined as follows:
 * {{{
 *   parser : comment? whitespace? ('@' parentheses+)? templateContent
 *   templateContent : (importExpression | localMember | template | mixed)*
 *   templateOrLocalMemberDeclaration : '@' ((('lazy' whitespaceNoBreak+)? 'val' whitespaceNoBreak+) | ('var' whitespaceNoBreak+))? identifier squareBrackets? parentheses*
 *   localMember : templateOrLocalMemberDeclaration (' ' | '\t')* '=' (' ' | '\t') scalaBlock
 *   template : templateOrLocalMemberDeclaration (' ' | '\t')* '=' (' ' | '\t') '{' templateContent '}'
 *   mixed : (comment | scalaBlockDisplayed | forExpression | ifExpression | matchExpOrSafeExpOrExpr | caseExpression | plain) | ('{' mixed* '}')
 *   matchExpOrSafeExpOrExpr : (expression | safeExpression) (whitespaceNoBreak 'match' block)?
 *   scalaBlockDisplayed : scalaBlock
 *   scalaBlockChained : scalaBlock
 *   scalaBlock : '@' brackets
 *   importExpression : '@' 'import ' .* '\r'? '\n'
 *   caseExpression : (whitespace? 'case' .+ '=>' block whitespace?) | whitespace
 *   forExpression : '@' "for" parentheses block
 *   simpleExpr : methodCall expressionPart*
 *   complexExpr : parentheses
 *   safeExpression : '@' parentheses
 *   ifExpression : '@' "if" parentheses expressionPart (elseIfCall)* elseCall?
 *   elseCall : whitespaceNoBreak? "else" whitespaceNoBreak? expressionPart
 *   elseIfCall : whitespaceNoBreak? "else if" parentheses whitespaceNoBreak? expressionPart
 *   chainedMethods : ('.' methodCall)+
 *   expressionPart : chainedMethods | block | (whitespaceNoBreak scalaBlockChained) | parentheses
 *   expression : '@' methodCall expressionPart*
 *   methodCall : identifier squareBrackets? parentheses?
 *   blockArgs : [^'=>' '\n']* '=>'
 *   block : whitespaceNoBreak? '{' blockArgs? mixed* '}'
 *   brackets : '{' (brackets | [^'}'])* '}'
 *   comment : '@*' [^'*@']* '*@'
 *   parentheses : '(' (parentheses | [^')'])* ')'
 *   squareBrackets : '[' (squareBrackets | [^']'])* ']'
 *   plain : ('@@' | '@}' | ([^'@'] [^'{' '}']))+
 *   whitespaceNoBreak : [' ' '\t']+
 *   identifier : javaIdentStart javaIdentPart* // see java docs for what these two rules mean
 * }}}
 *
 * TwirlParser can detect several type of parse errors and provides line information. In all cases, the parser will
 * continue parsing the best it can after encountering an error. The following errors are what can be detected:
 *   - EOF found when more input was expected.
 *   - Unmatched curly braces
 *   - Missing blocks after case and match statements
 *   - Invalid ("alone") '@' symbols.
 */
class TwirlParser(val shouldParseInclusiveDot: Boolean) {
  import play.twirl.parser.TreeNodes._
  import scala.util.parsing.input.Positional

  sealed abstract class ParseResult
  case class Success(template: Template, input: Input)                        extends ParseResult
  case class Error(template: Template, input: Input, errors: List[PosString]) extends ParseResult

  case class Input() {
    private var offset_      = 0
    private var source_      = ""
    private var length_      = 1
    val regressionStatistics = new collection.mutable.HashMap[String, (Int, Int)]

    /** Peek at the current input. Does not check for EOF. */
    def apply(): Char = source_.charAt(offset_)

    /**
     * Peek `length` characters ahead. Does not check for EOF.
     * @return
     *   string from current offset upto current offset + `length`
     */
    def apply(length: Int): String = source_.substring(offset_, offset_ + length)

    /** Equivalent to `input(str.length) == str`. Does not check for EOF. */
    def matches(str: String): Boolean = {
      var i = 0
      val l = str.length
      while (i < l) {
        if (source_.charAt(offset_ + i) != str.charAt(i))
          return false
        i += 1
      }
      true
    }

    /** Advance input by one character */
    def advance(): Unit = offset_ += 1

    /** Advance input by `increment` number of characters */
    def advance(increment: Int): Unit = offset_ += increment

    /** Backtrack by `decrement` numner of characters */
    def regress(decrement: Int): Unit = offset_ -= decrement

    /** Backtrack to a known offset */
    def regressTo(offset: Int): Unit = offset_ = offset

    def isPastEOF(len: Int): Boolean = (offset_ + len - 1) >= length_

    def isEOF: Boolean = isPastEOF(1)

    def atEnd(): Boolean = isEOF

    def pos() = OffsetPosition(source_, offset_)

    def offset(): Int = offset_

    def source(): String = source_

    /** Reset the input to have the given contents */
    def reset(source: String): Unit = {
      offset_ = 0
      source_ = source
      length_ = source.length()
      regressionStatistics.clear()
    }
  }

  private val input: Input                      = new Input
  private val errorStack: ListBuffer[PosString] = ListBuffer()

  /**
   * Try to match `str` and advance `str.length` characters.
   *
   * Reports an error if the input does not match `str` or if `str.length` goes past the EOF.
   */
  def accept(str: String): Unit = {
    val len = str.length
    if (!input.isPastEOF(len) && input.matches(str))
      input.advance(len)
    else
      error("Expected '" + str + "' but found '" + (if (input.isPastEOF(len)) "EOF" else input(len)) + "'")
  }

  /**
   * Does `f` applied to the current peek return true or false? If true, advance one character.
   *
   * Will not advance if at EOF.
   *
   * @return
   *   true if advancing, false otherwise.
   */
  def check(f: Char => Boolean): Boolean = {
    if (!input.isEOF && f(input())) {
      input.advance()
      true
    } else false
  }

  /**
   * Does the current input match `str`? If so, advance `str.length`.
   *
   * Will not advance if `str.length` surpasses EOF
   *
   * @return
   *   true if advancing, false otherwise.
   */
  def check(str: String): Boolean = {
    val len = str.length
    if (!input.isPastEOF(len) && input.matches(str)) {
      input.advance(len)
      true
    } else false
  }

  def error(message: String, offset: Int = input.offset()): Unit = {
    errorStack += position(PosString(message), offset)
  }

  /** Consume/Advance `length` characters, and return the consumed characters. Returns "" if at EOF. */
  def any(length: Int = 1): String = {
    if (input.isEOF) {
      error("Expected more input but found 'EOF'")
      ""
    } else {
      val s = input(length)
      input.advance(length)
      s
    }
  }

  /**
   * Consume characters until input matches `stop`
   *
   * @param inclusive
   *   - should stop be included in the consumed characters?
   * @return
   *   the consumed characters
   */
  def anyUntil(stop: String, inclusive: Boolean): String = {
    val sb = new StringBuilder
    while (!input.isPastEOF(stop.length) && !input.matches(stop)) sb.append(any())
    if (inclusive && !input.isPastEOF(stop.length))
      sb.append(any(stop.length))
    sb.toString()
  }

  /**
   * Consume characters until `f` returns false on the peek of input.
   *
   * @param inclusive
   *   - should the stopped character be included in the consumed characters?
   * @return
   *   the consumed characters
   */
  def anyUntil(f: Char => Boolean, inclusive: Boolean): String = {
    val sb = new StringBuilder
    while (!input.isEOF && !f(input())) sb.append(any())
    if (inclusive && !input.isEOF)
      sb.append(any())
    sb.toString
  }

  /** Set the source position of a Positional */
  def position[T <: Positional](positional: T, offset: Int): T = {
    if (positional != null)
      positional.setPos(OffsetPosition(input.source(), offset))
    positional
  }

  /**
   * Recursively match pairs of prefixes and suffixes and return the consumed characters
   *
   * Terminates at EOF.
   */
  def recursiveTag(prefix: String, suffix: String, allowStringLiterals: Boolean = false): String = {
    if (check(prefix)) {
      var stack = 1
      val sb    = new StringBuffer
      sb.append(prefix)
      while (stack > 0) {
        if (check(prefix)) {
          stack += 1
          sb.append(prefix)
        } else if (check(suffix)) {
          stack -= 1
          sb.append(suffix)
        } else if (input.isEOF) {
          error("Expected '" + suffix + "' but found 'EOF'")
          stack = 0
        } else if (allowStringLiterals) {
          stringLiteral("\"", "\\") match {
            case null => sb.append(any())
            case s    => sb.append(s)
          }
        } else {
          sb.append(any())
        }
      }
      sb.toString
    } else null
  }

  /**
   * Match a string literal, allowing for escaped quotes. Terminates at EOF.
   */
  def stringLiteral(quote: String, escape: String): String = {
    if (check(quote)) {
      var within = true
      val sb     = new StringBuffer
      sb.append(quote)
      while (within) {
        if (check(quote)) { // end of string literal
          sb.append(quote)
          within = false
        } else if (check(escape)) {
          sb.append(escape)
          if (check(quote)) { // escaped quote
            sb.append(quote)
          } else if (check(escape)) { // escaped escape
            sb.append(escape)
          }
        } else if (input.isEOF) {
          error("Expected '" + quote + "' but found 'EOF'")
          within = false
        } else {
          sb.append(any())
        }
      }
      sb.toString
    } else null
  }

  /** Match zero or more `parser` */
  def several[T, BufferType <: mutable.Buffer[T]](parser: () => T, provided: BufferType = null)(implicit
      manifest: ClassTag[BufferType]
  ): BufferType = {
    val ab =
      if (provided != null) provided else manifest.runtimeClass.getConstructor().newInstance().asInstanceOf[BufferType]
    var parsed = parser()
    while (parsed != null) {
      ab += parsed
      parsed = parser()
    }
    ab
  }

  def parentheses(): String = recursiveTag("(", ")", allowStringLiterals = true)

  def squareBrackets(): String = recursiveTag("[", "]")

  def whitespace(): String = anyUntil(_ > '\u0020', inclusive = false)

  // not completely faithful to original because it allows for zero whitespace
  def whitespaceNoBreak(): String = anyUntil(c => c != ' ' && c != '\t', inclusive = false)

  def identifier(): String = {
    var result: String = null
    // TODO: should I be checking for EOF here?
    if (!input.isEOF && Character.isJavaIdentifierStart(input())) {
      result = anyUntil(Character.isJavaIdentifierPart(_) == false, inclusive = false)
    }
    result
  }

  /**
   * Parse a comment.
   */
  def comment(): Comment = {
    val pos = input.offset()
    if (check("@*")) {
      val text = anyUntil("*@", inclusive = false)
      accept("*@")
      position(Comment(text), pos)
    } else null
  }

  /**
   * Parses comments and/or whitespace, ignoring both until the last comment is reached, and returning that (if found)
   */
  def lastComment(): Comment = {
    @tailrec
    def tryNext(last: Comment): Comment = {
      whitespace()
      val next = comment()
      if (next == null) {
        last
      } else {
        tryNext(next)
      }
    }
    tryNext(null)
  }

  def importExpression(): Simple = {
    val p = input.offset()
    if (check("@import "))
      position(Simple("import " + anyUntil("\n", inclusive = true).trim), p + 1) // don't include position of @
    else null
  }

  def localMember(): LocalMember = {
    var result: LocalMember = null
    val resetPosition       = input.offset()
    val templDecl           = templateOrLocalMemberDeclaration()
    if (templDecl != null) {
      anyUntil(c => c != ' ' && c != '\t', inclusive = false)
      var next = ""
      if (check(":")) {
        next = ":"
      } else if (check("=")) {
        next = "="
      }
      if (next == ":" || next == "=") {
        var resultType: Option[PosString] = None
        if (next == ":") {
          anyUntil(c => c != ' ' && c != '\t', inclusive = false)
          val resultTypePos = input.offset()
          val rt            = identifier() match {
            case null => null
            case id   => id
          }
          if (rt != null) {
            val types = Option(squareBrackets()).getOrElse("")
            resultType = Some(position(PosString(rt + types), resultTypePos))

            anyUntil(c => c != ' ' && c != '\t', inclusive = false)
            if (check("=")) {
              next = "="
            } else {
              next = ""
            }
          } else {
            next = ""
          }
        }
        if (next == "=") {
          anyUntil(c => c != ' ' && c != '\t', inclusive = false)
          val code = scalaBlock()
          if (code != null) {
            result = position(
              templDecl._3.fold(
                isVar =>
                  if (isVar) Var(templDecl._1, resultType, code) else Def(templDecl._1, templDecl._2, resultType, code),
                valIsLazy => Val(templDecl._1, valIsLazy, resultType, code)
              ),
              resetPosition
            )
          }
        }
      }
    }

    if (result == null)
      input.regressTo(resetPosition)
    result
  }

  def scalaBlock(): Simple = {
    if (check("@{")) {
      input.regress(1); // we need to parse the '{' via 'brackets()'
      val p = input.offset()
      brackets() match {
        case null => null
        case b    => position(Simple(b), p)
      }
    } else null
  }

  def brackets(): String = {
    var result = recursiveTag("{", "}")
    // mimicking how the original parser handled EOF for this rule
    if (result != null && result.last != '}')
      result = null
    result
  }

  def mixed(
      previousDefinedLocalMembersInParents: ArrayBuffer[LocalMember],
      previousDefinedTemplatesInParents: ArrayBuffer[SubTemplate]
  ): ListBuffer[TemplateTree] = {
    // parses: comment | scalaBlockDisplayed | forExpression | ifExpression | matchExpOrSafeExpOrExpr | caseExpression | plain
    def opt1(): ListBuffer[TemplateTree] = {
      val t =
        comment() match {
          case null =>
            scalaBlockDisplayed() match {
              case null =>
                forExpression() match {
                  case null =>
                    ifExpression(previousDefinedLocalMembersInParents, previousDefinedTemplatesInParents) match {
                      case null =>
                        matchExpOrSafeExpOrExpr() match {
                          case null =>
                            caseExpression() match {
                              case null => plain()
                              case x    => x
                            }
                          case x => x
                        }
                      case x => x
                    }
                  case x => x
                }
              case x => x
            }
          case x => x
        }
      if (t != null) ListBuffer(t)
      else null
    }

    // parses: '{' mixed* '}'
    def opt2(): ListBuffer[TemplateTree] = {
      val lbracepos = input.offset()
      if (check("{")) {
        var buffer = new ListBuffer[TemplateTree]
        buffer += position(Plain("{"), lbracepos)
        for (
          m <- several[ListBuffer[TemplateTree], ListBuffer[ListBuffer[TemplateTree]]] { () =>
            mixed(previousDefinedLocalMembersInParents, previousDefinedTemplatesInParents)
          }
        )
          buffer =
            buffer ++ m // creates a new object, but is constant in time, as opposed to buffer ++= m which is linear (proportional to size of m)
        val rbracepos = input.offset()
        if (check("}"))
          buffer += position(Plain("}"), rbracepos)
        else
          error("Expected ending '}'")
        buffer
      } else null
    }

    opt1() match {
      case null => opt2()
      case x    => x
    }
  }

  def scalaBlockDisplayed(): Display = {
    val sb = scalaBlock()
    if (sb != null)
      Display(ScalaExp(sb :: Nil))
    else
      null
  }

  def blockArgs(): PosString = {
    def noCurlyBraces(result: String): Boolean        = !result.contains("{") && !result.contains("}")
    def noOpeningParenthesis(result: String): Boolean = {
      val noContainsOpenParenthesis = !result.contains("(")

      val startsWithParenthesis = result.trim.startsWith("(") || result.trim.stripPrefix("case").trim.startsWith("(")
      val endsWithParenthesis   = result.stripSuffix("=>").trim.endsWith(")")

      noContainsOpenParenthesis || (startsWithParenthesis && endsWithParenthesis)
    }

    val p      = input.offset()
    val result = anyUntil("=>", inclusive = true)
    if (result.endsWith("=>") && !result.contains("\n") && noCurlyBraces(result) && noOpeningParenthesis(result))
      position(PosString(result), p)
    else {
      input.regress(result.length())
      null
    }
  }

  def block(
      blockArgsAllowed: Boolean,
      parseContentAsTemplate: Boolean,
      previousDefinedLocalMembersInParents: ArrayBuffer[LocalMember],
      previousDefinedTemplatesInParents: ArrayBuffer[SubTemplate]
  ): Block = {
    var result: Block = null
    val p             = input.offset()
    val ws            = whitespaceNoBreak()
    if (check("{")) {
      val blkArgs     = if (blockArgsAllowed) Option(blockArgs()) else None
      val blkContents =
        if (parseContentAsTemplate)
          templateContent(previousDefinedLocalMembersInParents, previousDefinedTemplatesInParents)
        else
          (
            Seq.empty, // no imports
            Seq.empty, // no localMembers
            Seq.empty, // no (sub)templates
            several[ListBuffer[TemplateTree], ListBuffer[ListBuffer[TemplateTree]]] { () =>
              mixed(previousDefinedLocalMembersInParents, previousDefinedTemplatesInParents)
            }.flatten // TODO - not use flatten here (if it's a performance problem)
          )
      accept("}")
      result =
        position(Block(ws, blkArgs, BlockTemplate(blkContents._1, blkContents._2, blkContents._3, blkContents._4)), p)
    } else {
      input.regressTo(p)
    }

    result
  }

  def caseExpression(): TemplateTree = {
    var result: TemplateTree = null

    val wspos = input.offset()
    val ws    = whitespace()
    val p     = input.offset()
    if (check("case ")) {
      val pattern = position(Simple("case " + anyUntil("=>", inclusive = true)), p)
      val blk     = block(blockArgsAllowed = true, parseContentAsTemplate = false, ArrayBuffer.empty, ArrayBuffer.empty)
      if (blk != null) {
        result = ScalaExp(ListBuffer(pattern, blk))
        whitespace()
      } else {
        // error("Expected block after 'case'")
        input.regressTo(wspos)
      }
    } else if (ws.length > 0) {
      // We could regress here and not return something for the ws, because the plain production rule
      // would parse this, but this would actually be a hotspot for backtracking, so let's return it
      // here seeing as it's been parsed all ready.
      result = position(Plain(ws), wspos)
    }

    result
  }

  def matchExpOrSafeExpOrExpr(): Display = {
    val result =
      expression() match {
        case null => safeExpression()
        case x    => x
      }

    if (result != null) {
      val exprs = result.exp.parts.asInstanceOf[ListBuffer[ScalaExpPart]]
      val mpos  = input.offset()
      val ws    = whitespaceNoBreak()
      if (check("match")) {
        val m   = position(Simple(ws + "match"), mpos)
        val blk = block(blockArgsAllowed = false, parseContentAsTemplate = false, ArrayBuffer.empty, ArrayBuffer.empty)
        if (blk != null) {
          exprs.append(m)
          exprs.append(blk)
        } else {
          // error("expected block after match")
          input.regressTo(mpos)
        }
      } else {
        input.regressTo(mpos)
      }
    }

    result
  }

  def forExpression(): Display = {
    var result: Display = null
    val p               = input.offset()
    if (check("@for")) {
      val parens = parentheses()
      if (parens != null) {
        val blk = block(blockArgsAllowed = true, parseContentAsTemplate = false, ArrayBuffer.empty, ArrayBuffer.empty)
        if (blk != null) {
          result = Display(
            ScalaExp(ListBuffer(position(Simple("for" + parens + " yield "), p + 1), blk))
          ) // don't include pos of @
        }
      }
    }

    if (result == null)
      input.regressTo(p)

    result
  }

  def safeExpression(): Display = {
    if (check("@(")) {
      input.regress(1)
      val p = input.offset()
      Display(ScalaExp(ListBuffer(position(Simple(parentheses()), p))))
    } else null
  }

  def plain(): Plain = {
    def single(): String = {
      if (check("@@")) "@"
      else if (check("@}")) "}"
      else if (!input.isEOF && input() != '@' && input() != '}' && input() != '{') any()
      else null
    }
    val p             = input.offset()
    var result: Plain = null
    var part          = single()
    if (part != null) {
      val sb = new StringBuffer
      while (part != null) {
        sb.append(part)
        part = single()
      }
      result = position(Plain(sb.toString), p)
    }

    result
  }

  def expression(): Display = {
    var result: Display = null
    if (check("@")) {
      val pos  = input.offset()
      val code = methodCall()
      if (code != null) {
        val parts = several[ScalaExpPart, ListBuffer[ScalaExpPart]] { () =>
          expressionPart(
            blockArgsAllowed = true,
            chainedMethodsAllowed = true,
            scalaBlockChainedAllowed = false,
            whitespaceBeforeSimpleParensAllowed = false,
            parseBlockContentAsTemplate = false,
            ArrayBuffer.empty,
            ArrayBuffer.empty,
          )
        }
        parts.prepend(position(Simple(code), pos))
        result = Display(ScalaExp(parts))
      } else input.regressTo(pos - 1) // don't consume the @
    }

    result
  }

  def methodCall(): String = {
    val name = identifier()
    if (name != null) {
      val sb = new StringBuffer(name)
      sb.append(Option(squareBrackets()).getOrElse(""))
      sb.append(Option(parentheses()).getOrElse(""))
      sb.toString
    } else null
  }

  def expressionPart(
      blockArgsAllowed: Boolean,
      chainedMethodsAllowed: Boolean,
      scalaBlockChainedAllowed: Boolean,
      whitespaceBeforeSimpleParensAllowed: Boolean,
      parseBlockContentAsTemplate: Boolean,
      previousDefinedLocalMembersInParents: ArrayBuffer[LocalMember],
      previousDefinedTemplatesInParents: ArrayBuffer[SubTemplate],
  ): ScalaExpPart = {
    def simpleParens() = {
      val p = input.offset()
      if (whitespaceBeforeSimpleParensAllowed) {
        whitespaceNoBreak()
      }
      val parens = parentheses()
      if (parens != null) position(Simple(parens), p)
      else null
    }

    def wsThenScalaBlockChained() = {
      val reset = input.offset()
      whitespaceNoBreak()
      val chained = scalaBlockChained()
      if (chained eq null) input.regressTo(reset)
      chained
    }

    (if (chainedMethodsAllowed) chainedMethods() else null) match {
      case null =>
        block(
          blockArgsAllowed,
          parseContentAsTemplate = parseBlockContentAsTemplate,
          previousDefinedLocalMembersInParents,
          previousDefinedTemplatesInParents
        ) match {
          case null =>
            (if (scalaBlockChainedAllowed) wsThenScalaBlockChained() else null) match {
              case null => simpleParens()
              case x    => x
            }
          case x => x
        }
      case x => x
    }
  }

  def scalaBlockChained(): Block = {
    val blk = scalaBlock()
    if (blk != null)
      Block("", None, BlockTemplate(Seq.empty, Seq.empty, Seq.empty, ListBuffer(ScalaExp(ListBuffer(blk)))))
    else null
  }

  // varies from original parser in that it can accept a trailing '.'
  def chainedMethods(): Simple = {
    def inclusiveDot(): Simple = {
      val p = input.offset()
      if (check(".")) {
        val sb = new StringBuffer(".")

        // Simply alternate between matching a methodCall and a dot until one fails.
        var done            = false
        var matchMethodCall = true // represent: "should I try to match a method call or a dot?
        while (!done) {
          if (matchMethodCall) {
            val method = methodCall()
            if (method != null)
              sb.append(method)
            else done = true
          } else {
            if (check("."))
              sb.append('.')
            else done = true
          }
          matchMethodCall = !matchMethodCall
        }
        position(Simple(sb.toString), p)
      } else null
    }

    // The logic of this method is as follow:
    // We know we must start with a methodCall, so try to parse one.
    // If it exceeds, enter a loop trying to parse a dot and methodcall in each iteration.
    def exclusiveDot(): Simple = {
      val p              = input.offset()
      var result: Simple = null
      if (check(".")) {
        val firstMethodCall = methodCall()
        if (firstMethodCall != null) {
          val sb   = new StringBuffer("." + firstMethodCall)
          var done = false
          while (!done) {
            val reset            = input.offset()
            var nextLink: String = null
            if (check(".")) {
              methodCall() match {
                case m: String => nextLink = m
                case null      =>
              }
            }

            nextLink match {
              case null =>
                done = true
                input.regressTo(reset)
              case _ =>
                sb.append(".")
                sb.append(nextLink)
            }
          }

          result = position(Simple(sb.toString), p)
        } else input.regressTo(p)
      }

      result
    }

    if (shouldParseInclusiveDot)
      inclusiveDot()
    else
      exclusiveDot()
  }

  def ifExpression(
      previousDefinedLocalMembersInParents: ArrayBuffer[LocalMember],
      previousDefinedTemplatesInParents: ArrayBuffer[SubTemplate],
  ): Display = {
    val result: ListBuffer[ScalaExpPart] = ListBuffer.empty
    val defaultElse                      = Simple(" else {null} ")
    val p                                = input.offset()
    var positional: Simple               = null
    if (check("@if")) {
      val parens = parentheses()
      if (parens != null) {
        val blk =
          expressionPart(
            blockArgsAllowed = true,
            chainedMethodsAllowed = false,
            scalaBlockChainedAllowed = true,
            whitespaceBeforeSimpleParensAllowed = true,
            parseBlockContentAsTemplate = true,
            previousDefinedLocalMembersInParents,
            previousDefinedTemplatesInParents,
          )
        if (blk != null) {
          positional = Simple("if" + parens)
          result += Simple("if" + parens)
          result += blk

          val elseIfCallParts = several[Seq[ScalaExpPart], ArrayBuffer[Seq[ScalaExpPart]]] { () =>
            elseIfCall(previousDefinedLocalMembersInParents, previousDefinedTemplatesInParents)
          }
          result ++= elseIfCallParts.flatten

          val elseCallPart = elseCall(previousDefinedLocalMembersInParents, previousDefinedTemplatesInParents)
          if (elseCallPart == null) {
            result += defaultElse
          } else {
            result ++= elseCallPart
          }
        } else {
          error("Expected '{ ... }', '@{ ... }' or '(...)' after 'if(...)'", p)
        }
      }
    }

    if (result.isEmpty) {
      input.regressTo(p)
      null
    } else {
      position(positional, p + 1)
      Display(ScalaExp(result))
    }
  }

  def elseIfCall(
      previousDefinedLocalMembersInParents: ArrayBuffer[LocalMember],
      previousDefinedTemplatesInParents: ArrayBuffer[SubTemplate],
  ): Seq[ScalaExpPart] = {
    val reset = input.offset()
    whitespaceNoBreak()
    if (check("else if")) {
      whitespaceNoBreak()
      val args = parentheses()
      if (args != null) {
        val afterElseIfPos = input.offset()
        whitespaceNoBreak()
        if (check("@@") || (!check("{") && !check("@") && !check("("))) {
          // We only parse `else if(...)` if it is one of the following (with 0 to n whitespaces allowed):
          // - `else if(...) {`
          // - `else if(...) (`
          // - `else if(...) @`
          input.regressTo(reset)
          return null
        }
        input.regressTo(afterElseIfPos) // above is just a check, we are not consuming
        val blk =
          expressionPart(
            blockArgsAllowed = true,
            chainedMethodsAllowed = false,
            scalaBlockChainedAllowed = true,
            whitespaceBeforeSimpleParensAllowed = true,
            parseBlockContentAsTemplate = true,
            previousDefinedLocalMembersInParents,
            previousDefinedTemplatesInParents,
          )
        if (blk != null) {
          Seq(Simple("else if" + args), blk)
        } else {
          error(
            "Expected '{ ... }', '@{ ... }' or '(...)' after 'else if(...)'. Hint: To ignore 'else if...' and render it as plain string instead you can escape @ with @@.",
            reset
          )
          null
        }
      } else {
        input.regressTo(reset) // Don't swallow 'else if'
        null
      }
    } else {
      input.regressTo(reset)
      null
    }
  }

  def elseCall(
      previousDefinedLocalMembersInParents: ArrayBuffer[LocalMember],
      previousDefinedTemplatesInParents: ArrayBuffer[SubTemplate],
  ): Seq[ScalaExpPart] = {
    val reset = input.offset()
    whitespaceNoBreak()
    if (check("else")) {
      whitespaceNoBreak()
      val afterElsePos = input.offset()
      if (check("@@") || (!check("{") && !check("@") && !check("("))) {
        // We only parse `else` if it is one of the following (with 0 to n whitespaces allowed):
        // - `else {`
        // - `else (`
        // - `else @`
        input.regressTo(reset)
        return null
      }
      input.regressTo(afterElsePos) // above is just a check, we are not consuming
      val blk = expressionPart(
        blockArgsAllowed = true,
        chainedMethodsAllowed = false,
        scalaBlockChainedAllowed = true,
        whitespaceBeforeSimpleParensAllowed = true,
        parseBlockContentAsTemplate = true,
        previousDefinedLocalMembersInParents,
        previousDefinedTemplatesInParents,
      )
      if (blk != null) {
        Seq(Simple("else"), blk)
      } else {
        error(
          "Expected '{ ... }', '@{ ... }' or '(...)' after 'else'. Hint: To ignore 'else...' and render it as plain string instead you can escape @ with @@.",
          reset
        )
        null
      }
    } else {
      input.regressTo(reset)
      null
    }
  }

  def template(
      previousDefinedLocalMembersInParents: ArrayBuffer[LocalMember],
      previousDefinedTemplatesInParents: ArrayBuffer[SubTemplate]
  ): SubTemplate = {
    var result: SubTemplate = null
    val resetPosition       = input.offset()
    val templDecl           = templateOrLocalMemberDeclaration()
    if (templDecl != null) {
      anyUntil(c => c != ' ' && c != '\t', inclusive = false)
      if (check("=")) {
        anyUntil(c => c != ' ' && c != '\t', inclusive = false)
        if (check("{")) {
          val (imports, localMembers, templates, mixeds) =
            templateContent(previousDefinedLocalMembersInParents, previousDefinedTemplatesInParents)
          if (check("}"))
            result = position(
              SubTemplate(
                templDecl._3,
                templDecl._1,
                templDecl._2,
                imports,
                localMembers,
                templates,
                mixeds
              ),
              resetPosition
            )
        }
      }
    }

    if (result == null)
      input.regressTo(resetPosition)
    result
  }

  def templateOrLocalMemberDeclaration(): (PosString, PosString, Either[Boolean, Boolean]) = {
    val resetPosition = input.offset()
    if (check("@")) {
      val lazypos = input.offset()
      val isLazy  = check("lazy") && !whitespaceNoBreak().isEmpty
      if (!isLazy) {
        // The word 'lazy' could be parsed, but no whitespace afterwards -> isLazy is false, but the pointer moved forward...
        input.regressTo(lazypos)
      }
      val valorvarpos = input.offset()
      val isVal       = check("val") && !whitespaceNoBreak().isEmpty
      if (!isVal) {
        // The word 'val' could be parsed, but no whitespace afterwards -> isVal is false, but the pointer moved forward...
        input.regressTo(valorvarpos)
      }
      val isVar = !isVal && check("var") && !whitespaceNoBreak().isEmpty
      if (!isVal && !isVar) {
        // The word 'var' could be parsed, but no whitespace afterwards -> isVar is false, but the pointer moved forward...
        input.regressTo(valorvarpos)
      }
      if (isLazy && isVar) {
        input.regressTo(resetPosition) // don't consume @
        error("'lazy' not allowed here. Only val definitions can be lazy.", lazypos)
        return null
      }
      if (isLazy && !isVal) {
        input.regressTo(resetPosition) // don't consume @
        error("Expected 'val' after 'lazy'", lazypos)
        return null
      }
      val namepos = input.offset()
      val name    = identifier() match {
        case null => null
        case id   => position(PosString(id), namepos)
      }

      if (name != null) {
        val paramspos = input.offset()
        val types     = Option(squareBrackets()).getOrElse("")
        if (types.replaceAll("\\s", "") == "[]") {
          input.regressTo(resetPosition) // don't consume @
          error(s"identifier expected but ']' found", paramspos)
          return null
        }
        if ((isVal || isVar) && !types.isEmpty) {
          input.regressTo(resetPosition) // don't consume @
          error(
            s"Invalid variable definition: '${name.str}' cannot have type parameters.",
            namepos
          )
          return null
        }
        val args = several[String, ArrayBuffer[String]] { () => parentheses() }
        if ((isVal || isVar) && !args.isEmpty) {
          input.regressTo(resetPosition) // don't consume @
          error(
            s"Invalid variable definition: '${name.str}' cannot have parameter lists.",
            namepos
          )
          return null
        }
        val params = position(PosString(types + args.mkString), paramspos)
        if (params != null)
          return (name, params, Either.cond[Boolean, Boolean](isVal, isLazy, isVar))
      } else input.regressTo(resetPosition) // don't consume @
    }

    null
  }

  def templateContent(
      previousDefinedLocalMembersInParents: ArrayBuffer[LocalMember],
      previousDefinedTemplatesInParents: ArrayBuffer[SubTemplate]
  ): (
      collection.Seq[Simple],
      collection.Seq[LocalMember],
      collection.Seq[SubTemplate],
      collection.Seq[TemplateTree]
  ) = {
    val imports      = new ArrayBuffer[Simple]
    val localMembers = new ArrayBuffer[LocalMember]
    val templates    = new ArrayBuffer[SubTemplate]
    val mixeds       = new ArrayBuffer[TemplateTree]

    var done = false
    while (!done) {
      val impExp = importExpression()
      if (impExp != null) imports += impExp
      else {
        def nameAlreadyDefinedMsg(name: String) =
          s"$name is already defined. To reassign $name, remove any argument lists or type parameters. Otherwise choose a different name."
        val memberPosition                                 = input.offset()
        def varWithSameNameAlreadyDefined(name: PosString) =
          (previousDefinedLocalMembersInParents ++ localMembers).exists(_ match {
            case Var(varname, _, _) if name.str == varname.str => true
            case _                                             => false
          })
        val lmemberOrVarReassignment = localMember() match {
          case Def(name, params, resultType, code) if varWithSameNameAlreadyDefined(name) => {
            if (!params.str.isEmpty) {
              input.regressTo(memberPosition)
              error(nameAlreadyDefinedMsg(name.str), memberPosition)
              null
            } else if (!resultType.map(_.str.isEmpty).getOrElse(true)) {
              input.regressTo(memberPosition)
              error("Type annotation is not allowed on variable reassignment.", memberPosition)
              null
            } else {
              val reassignment = position(Reassignment(Right(Var(name, None, code))), memberPosition)
              mixeds += reassignment
              reassignment
            }
          }
          case lmember if lmember != null => {
            localMembers += lmember
            lmember
          }
          case _ => null
        }
        if (lmemberOrVarReassignment == null) {
          def tmplVarWithSameNameAlreadyDefined(name: PosString) =
            (previousDefinedTemplatesInParents ++ templates).exists(_ match {
              case SubTemplate(declaration, varname, _, _, _, _, _)
                  if name.str == varname.str && declaration.left.exists(_ == true) => // same name && "var"
                true
              case _ => false
            })
          val templatePosition          = input.offset()
          val templateOrVarReassignment = template(
            previousDefinedLocalMembersInParents ++ localMembers,
            previousDefinedTemplatesInParents ++ templates
          ) match {
            case tmpl @ SubTemplate(declaration, name, params, _, _, _, _)
                if declaration.left // we only care about `@name = { ... }`, meaning no `var`, `val` or `lazy` keyword was given,
                  .exists(_ == false) && // so it would be a `def` if there wouldn't be a var with same name to reassign
                  tmplVarWithSameNameAlreadyDefined(name) => {
              if (!params.str.isEmpty) {
                input.regressTo(templatePosition)
                error(nameAlreadyDefinedMsg(name.str), templatePosition)
                null
              } else {
                val reassignment = position(Reassignment(Left(tmpl)), templatePosition)
                mixeds += reassignment
                reassignment
              }
            }
            case templ if templ != null => {
              templates += templ
              templ
            }
            case _ => null
          }
          if (templateOrVarReassignment == null) {
            val mix = mixed(
              previousDefinedLocalMembersInParents ++ localMembers,
              previousDefinedTemplatesInParents ++ templates
            )
            if (mix != null) mixeds ++= mix
            else {
              // check for an invalid '@' symbol, and just skip it so we can continue the parse
              val pos = input.offset()
              if (check("@")) error("Invalid '@' symbol", pos)
              else done = true
            }
          }
        }
      }
    }

    (imports, localMembers, templates, mixeds)
  }

  def extraImports(): collection.Seq[Simple] = {
    var resetPosition = input.offset()
    val imports       = new ArrayBuffer[Simple]

    lastComment()

    var done = false
    while (!done) {
      val importExp = importExpression()
      if (importExp ne null) {
        imports += importExp
        resetPosition = input.offset()
        lastComment()
      } else {
        done = true
      }
    }

    input.regressTo(resetPosition)

    imports
  }

  /**
   * Parse the template arguments.
   */
  private def templateArgs(): String = {
    val result = several[String, ArrayBuffer[String]] { () => parentheses() }
    if (result.nonEmpty)
      result.mkString
    else
      null
  }

  /**
   * Parse the template arguments, if they exist
   */
  private def maybeTemplateArgs(): Option[PosString] = {
    val reset                          = input.offset()
    def parseArgs(): Option[PosString] = {
      val p    = input.offset()
      val args = templateArgs()
      if (args != null) {
        val result = position(PosString(args), p)
        check("\n")
        Some(result)
      } else {
        None
      }
    }
    if (check("@(")) {
      input.regress(1)
      parseArgs()
    } else if (check("@[")) {
      input.regress(1)
      Option(squareBrackets()) match {
        case Some(value) if value.replaceAll("\\s", "") == "[]" =>
          input.regressTo(reset)                             // don't consume @
          error(s"identifier expected but ']' found", reset) // TODO: really reset hier?
          None
        case Some(types) =>
          parseArgs() match {
            case Some(value) => Some(position(PosString(types + value.str), reset))
            case None        =>
              val result = Some(position(PosString(types + "()"), reset))
              check("\n")
              result
          }
        case None =>
          input.regressTo(reset)                      // don't consume @
          error(s"Type parameter(s) expected", reset) // TODO: really reset hier?
          None
      }
    } else None
  }

  /**
   * Parse the template arguments, if they exist
   */
  private def constructorArgs(): PosString = {
    if (check("@this(")) {
      input.regress(1)
      val p    = input.offset()
      val args = templateArgs()
      if (args != null) position(PosString(args), p)
      else null
    } else null
  }

  def parse(source: String): ParseResult = {
    // Initialize mutable state
    input.reset(source)
    errorStack.clear()

    val topImports                 = extraImports()
    val (constructor, argsComment) = {
      val constructorComment = Option(lastComment())
      whitespace()
      val constructor = constructorArgs()
      if (constructor != null) {
        // progress to try and parse comment for args
        whitespace()
        val argsComment = Option(lastComment())
        whitespace()
        (Some(Constructor(constructorComment, constructor)), argsComment)
      } else {
        (None, constructorComment)
      }
    }
    val args                                       = maybeTemplateArgs()
    val (imports, localMembers, templates, mixeds) = templateContent(ArrayBuffer.empty, ArrayBuffer.empty)

    val template = Template(
      constructor,
      argsComment,
      args.getOrElse(PosString("()")),
      topImports,
      imports,
      localMembers,
      templates,
      mixeds
    )

    if (errorStack.isEmpty)
      Success(template, input)
    else
      Error(template, input, errorStack.toList)
  }

  def mkRegressionStatisticsString(): Unit = {
    val a = input.regressionStatistics.toArray.sortBy { case (_, (c, _)) => c }
    a.mkString("\n")
  }
}
