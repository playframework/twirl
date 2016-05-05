/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.twirl.parser

import scala.annotation.{tailrec, elidable}
import scala.annotation.elidable._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer
import scala.collection.mutable.BufferLike
import scala.collection.mutable.ListBuffer
import scala.util.parsing.input.OffsetPosition

/**
 * TwirlParser is a recursive descent parser for a modified grammar of the Play2 template language as loosely defined [[http://www.playframework.com/documentation/2.1.x/ here]] and more rigorously defined by the original template parser, `play.templates.ScalaTemplateCompiler.TemplateParser`.
 * TwirlParser is meant to be a near drop in replacement for `play.templates.ScalaTemplateCompiler.TemplateParser`.
 *
 * The original grammar, as reversed-engineered from `play.templates.ScalaTemplateCompiler.TemplateParser`, is defined as follows:
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
 *   importExpression : '@' 'import ' .* '\r'? '\n'
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
 * TwirlParser implements a slightly modified version of the above grammar that removes some back-tracking within the 'mixed' non-terminal. It is defined as follows:
 * {{{
 *   parser : comment? whitespace? ('@' parentheses+)? templateContent
 *   templateContent : (importExpression | localDef | template | mixed)*
 *   templateDeclaration : '@' identifier squareBrackets? parentheses*
 *   localDef : templateDeclaration (' ' | '\t')* '=' (' ' | '\t') scalaBlock
 *   template : templateDeclaration (' ' | '\t')* '=' (' ' | '\t') '{' templateContent '}'
 *   mixed : (comment | scalaBlockDisplayed | forExpression | matchExpOrSafeExpOrExpr | caseExpression | plain) | ('{' mixed* '}')
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
 *   elseCall : whitespaceNoBreak? "else" whitespaceNoBreak?
 *   chainedMethods : ('.' methodCall)+
 *   expressionPart : chainedMethods | block | (whitespaceNoBreak scalaBlockChained) | elseCall | parentheses
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
 * TwirlParser can detect several type of parse errors and provides line information. In all cases, the parser will continue parsing the best it can after encountering an error. The following errors are what can be detected:
 *   - EOF found when more input was expected.
 *   - Unmatched curly braces
 *   - Missing blocks after case and match statements
 *   - Invalid ("alone") '@' symbols.
 */

class TwirlParser(val shouldParseInclusiveDot: Boolean) {

  import play.twirl.parser.TreeNodes._
  import scala.util.parsing.input.Positional

  sealed abstract class ParseResult
  case class Success(template: Template, input: Input) extends ParseResult
  case class Error(template: Template, input: Input, errors: List[PosString]) extends ParseResult

  case class Input() {
    private var offset_ = 0
    private var source_ = ""
    private var length_ = 1
    val regressionStatistics = new collection.mutable.HashMap[String, (Int, Int)]

    /** Peek at the current input. Does not check for EOF. */
    def apply(): Char = source_.charAt(offset_)

    /**
     * Peek `length` characters ahead. Does not check for EOF.
     * @return string from current offset upto current offset + `length`
     */
    def apply(length: Int): String = source_.substring(offset_, (offset_ + length))

    /** Equivalent to `input(str.length) == str`. Does not check for EOF. */
    def matches(str: String): Boolean = {
      var i = 0;
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
    def regressTo(offset: Int): Unit = {
      @noinline @elidable(INFO)
      def updateRegressionStatistics() = {
        val distance = offset_ - offset
        val methodName = Thread.currentThread().getStackTrace()(2).getMethodName()
        val (count, charAccum) = regressionStatistics.get(methodName) getOrElse ((0, 0))
        regressionStatistics(methodName) = (count + 1, charAccum + distance)
      }

      offset_ = offset
    }

    def isPastEOF(len: Int): Boolean = (offset_ + len-1) >= length_

    def isEOF() = isPastEOF(1)

    def atEnd() = isEOF()

    def pos() = new OffsetPosition(source_, offset_)

    def offset() = offset_

    def source() = source_

    /** Reset the input to have the given contents */
    def reset(source: String) {
      offset_ = 0
      source_ = source
      length_ = source.length()
      regressionStatistics.clear()
    }
  }

  private val input: Input = new Input
  private val errorStack: ListBuffer[PosString] = ListBuffer()

  /**
   *  Try to match `str` and advance `str.length` characters.
   *
   *  Reports an error if the input does not match `str` or if `str.length` goes past the EOF.
   */
  def accept(str: String): Unit = {
    val len = str.length
    if (!input.isPastEOF(len) && input.matches(str))
      input.advance(len)
    else
      error("Expected '" + str + "' but found '" + (if (input.isPastEOF(len)) "EOF" else input(len)) + "'")
  }

  /**
   *  Does `f` applied to the current peek return true or false? If true, advance one character.
   *
   *  Will not advance if at EOF.
   *
   *  @return true if advancing, false otherwise.
   */
  def check(f: Char => Boolean): Boolean = {
    if (!input.isEOF() && f(input())) {
      input.advance()
      true
    } else false
  }

  /**
   *  Does the current input match `str`? If so, advance `str.length`.
   *
   *  Will not advance if `str.length` surpasses EOF
   *
   *  @return true if advancing, false otherwise.
   */
  def check(str: String): Boolean = {
    val len = str.length
    if (!input.isPastEOF(len) && input.matches(str)){
      input.advance(len)
      true
    } else false
  }

  def error(message: String, offset: Int = input.offset): Unit = {
    errorStack += position(PosString(message), offset)
  }

 /** Consume/Advance `length` characters, and return the consumed characters. Returns "" if at EOF. */
  def any(length: Int = 1): String = {
    if (input.isEOF()) {
      error("Expected more input but found 'EOF'")
      ""
    } else {
      val s = input(length)
      input.advance(length)
      s
    }
  }

  /**
   *  Consume characters until input matches `stop`
   *
   *  @param inclusive - should stop be included in the consumed characters?
   *  @return the consumed characters
   */
  def anyUntil(stop: String, inclusive: Boolean): String = {
    var sb = new StringBuilder
    while (!input.isPastEOF(stop.length) && !input.matches(stop))
      sb.append(any())
    if (inclusive && !input.isPastEOF(stop.length))
      sb.append(any(stop.length))
    sb.toString()
  }

  /**
   *  Consume characters until `f` returns false on the peek of input.
   *
   *  @param inclusive - should the stopped character be included in the consumed characters?
   *  @return the consumed characters
   */
  def anyUntil(f: Char => Boolean, inclusive: Boolean): String = {
    var sb = new StringBuilder
    while (!input.isEOF() && f(input()) == false)
      sb.append(any())
    if (inclusive && !input.isEOF())
      sb.append(any())
    sb.toString
  }

  /** Set the source position of a Positional */
  def position[T <: Positional](positional: T, offset: Int): T = {
    if (positional != null)
      positional.setPos(OffsetPosition(input.source, offset))
    positional
  }

  /** Recursively match pairs of prefixes and suffixes and return the consumed characters
   *
   *  Terminates at EOF.
   */
  def recursiveTag(prefix: String, suffix: String, allowStringLiterals: Boolean = false): String = {
    if (check(prefix)) {
      var stack = 1
      val sb = new StringBuffer
      sb.append(prefix)
      while (stack > 0) {
        if (check(prefix)) {
          stack += 1
          sb.append(prefix)
        } else if (check(suffix)) {
          stack -= 1
          sb.append(suffix)
        } else if (input.isEOF()) {
          error("Expected '" + suffix + "' but found 'EOF'")
          stack = 0
        } else if (allowStringLiterals) {
          stringLiteral("\"", "\\") match {
            case null => sb.append(any())
            case s => sb.append(s)
          }
        } else {
          sb.append(any())
        }
      }
      sb.toString()
    } else null
  }

  /**
   * Match a string literal, allowing for escaped quotes.
   * Terminates at EOF.
   */
  def stringLiteral(quote: String, escape: String): String = {
    if (check(quote)) {
      var within = true
      val sb = new StringBuffer
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
        } else if (input.isEOF()) {
          error("Expected '" + quote + "' but found 'EOF'")
          within = false
        } else {
          sb.append(any())
        }
      }
      sb.toString()
    } else null
  }

  /** Match zero or more `parser` */
  def several[T, BufferType <: Buffer[T]](parser: () => T, provided: BufferType = null)(implicit manifest: Manifest[BufferType]): BufferType = {
    val ab = if (provided != null) provided else manifest.runtimeClass.newInstance().asInstanceOf[BufferType]
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
    if (!input.isEOF() && Character.isJavaIdentifierStart(input())) {
      result = anyUntil(Character.isJavaIdentifierPart(_) == false, false)
    }
    result
  }

  /**
    * Parse a comment.
    */
  def comment(): Comment = {
    val pos = input.offset
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
    val p = input.offset
    if (check("@import "))
      position(Simple("import " + anyUntil("\n", inclusive = true).trim), p+1) // don't include position of @
    else null
  }

  def localDef(): Def = {
    var result: Def = null
    val resetPosition = input.offset
    val templDecl = templateDeclaration()
    if (templDecl != null) {
      anyUntil(c => c != ' ' && c != '\t', inclusive = false)
      if (check("=")) {
        anyUntil(c => c != ' ' && c != '\t', inclusive = false)
        val code = scalaBlock()
        if (code != null) {
          result = Def(templDecl._1, templDecl._2, code)
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
      val p = input.offset
      brackets() match {
        case null => null
        case b => position(Simple(b), p)
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

  def mixed(): ListBuffer[TemplateTree] = {
    // parses: comment | scalaBlockDisplayed | forExpression | matchExpOrSafeExprOrExpr | caseExpression | plain
    def opt1(): ListBuffer[TemplateTree] = {
      val t =
        comment() match {
          case null => scalaBlockDisplayed() match {
            case null => forExpression match {
              case null => matchExpOrSafeExpOrExpr() match {
                case null => caseExpression() match {
                  case null => plain()
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
        for (m <- several[ListBuffer[TemplateTree], ListBuffer[ListBuffer[TemplateTree]]](mixed))
          buffer = buffer ++ m // creates a new object, but is constant in time, as opposed to buffer ++= m which is linear (proportional to size of m)
        val rbracepos = input.offset
        if (check("}"))
          buffer += position(Plain("}"), rbracepos)
        else
          error("Expected ending '}'")
        buffer
      } else null
    }

    opt1() match {
      case null => opt2()
      case x => x
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
    val p = input.offset
    val result = anyUntil("=>", true)
    if (result.endsWith("=>") && !result.contains("\n"))
      position(PosString(result), p)
    else {
      input.regress(result.length())
      null
    }
  }

  def block(): Block = {
    var result: Block = null
    val p = input.offset
    val ws = whitespaceNoBreak()
    if (check("{")) {
      val blkArgs = Option(blockArgs())
      val mixeds = several[ListBuffer[TemplateTree], ListBuffer[ListBuffer[TemplateTree]]](mixed)
      accept("}")
      // TODO - not use flatten here (if it's a performance problem)
      result = position(Block(ws, blkArgs, mixeds.flatten), p)
    } else {
      input.regressTo(p)
    }

    result
  }

  def caseExpression(): TemplateTree = {
    var result: TemplateTree = null

    val wspos = input.offset
    val ws = whitespace()
    val p = input.offset()
    if (check("case ")) {
      val pattern = position(Simple("case " + anyUntil("=>", inclusive = true)), p)
      val blk = block()
      if (blk != null) {
        result = ScalaExp(ListBuffer(pattern, blk))
        whitespace()
      } else {
        //error("Expected block after 'case'")
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
    val resetPosition = input.offset
    val result =
      expression() match {
        case null => safeExpression()
        case x => x
      }

    if (result != null) {
      val exprs = result.exp.parts.asInstanceOf[ListBuffer[ScalaExpPart]]
      val mpos = input.offset
      val ws = whitespaceNoBreak()
      if (check("match")) {
        val m = position(Simple(ws + "match"), mpos)
        val blk = block()
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
    val p = input.offset
    if (check("@for")) {
      val parens = parentheses()
      if (parens != null) {
        val blk = block()
        if (blk != null) {
          result = Display(ScalaExp(ListBuffer(position(Simple("for" + parens + " yield "), p+1), blk))) // don't include pos of @
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
      val p = input.offset
      Display(ScalaExp(ListBuffer(position(Simple(parentheses()), p))))
    } else null
  }

  def plain(): Plain = {
    def single(): String = {
      if (check("@@")) "@"
      else if (check("@}")) "}"
      else if (!input.isEOF() && input() != '@' && input() != '}' && input() != '{') any()
      else null
    }
    val p = input.offset
    var result: Plain = null
    var part = single()
    if (part != null) {
      val sb = new StringBuffer
      while (part != null) {
        sb.append(part)
        part = single()
      }
      result = position(Plain(sb.toString()), p)
    }

    result
  }

  def expression(): Display = {
    var result: Display = null
    if (check("@")) {
      val pos = input.offset
      val code = methodCall()
      if (code != null) {
        val parts = several[ScalaExpPart, ListBuffer[ScalaExpPart]](expressionPart)
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
      sb.append(Option(squareBrackets) getOrElse "")
      sb.append(Option(parentheses) getOrElse "")
      sb.toString()
    } else null
  }

  def expressionPart(): ScalaExpPart = {
    def simpleParens() = {
      val p = input.offset
      val parens = parentheses()
      if (parens != null) position(Simple(parens), p)
      else null
    }

    def wsThenScalaBlockChained() = {
      val reset = input.offset
      val ws = whitespaceNoBreak()
      val chained = scalaBlockChained()
      if (chained eq null) input.regressTo(reset)
      chained
    }

    chainedMethods() match {
      case null => block() match {
        case null => wsThenScalaBlockChained() match {
          case null => elseCall() match {
            case null => simpleParens()
            case x => x
          }
          case x => x
        }
        case x => x
      }
      case x => x
    }
  }

  def scalaBlockChained(): Block = {
    val blk = scalaBlock()
    if (blk != null)
      Block("", None, ListBuffer(ScalaExp(ListBuffer(blk))))
    else null
  }

  // varies from original parser in that it can accept a trailing '.'
  def chainedMethods(): Simple = {
    def inclusiveDot(): Simple = {
      val p = input.offset
      if (check(".")) {
        val sb = new StringBuffer(".")

        // Simply alternate between matching a methodCall and a dot until one fails.
        var done = false
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
        position(Simple(sb.toString()), p)
      } else null
    }

    // The logic of this method is as follow:
    // We know we must start with a methodCall, so try to parse one.
    // If it exceeds, enter a loop trying to parse a dot and methodcall in each iteration.
    def exclusiveDot(): Simple = {
      val p = input.offset
      var result: Simple = null
      if (check(".")) {
        val firstMethodCall = methodCall()
        if (firstMethodCall != null) {
          val sb = new StringBuffer("." + firstMethodCall)
          var done = false
          while (!done) {
            val reset = input.offset
            var nextLink: String = null
            if (check(".")) {
              methodCall() match {
                case m: String => nextLink = m
                case _ =>
              }
            }

            nextLink match {
              case null => {
                done = true
                input.regressTo(reset)
              }
              case _ => {
                sb.append(".")
                sb.append(nextLink)
              }
            }
          }

          result = position(Simple(sb.toString()), p)
        } else input.regressTo(p)
      }

      result
    }

    if (shouldParseInclusiveDot)
      inclusiveDot()
    else
      exclusiveDot()
  }

  def elseCall(): Simple = {
    val reset = input.offset
    whitespaceNoBreak()
    val p = input.offset
    if (check("else")) {
      whitespaceNoBreak()
      position(Simple("else"), p)
    } else {
      input.regressTo(reset)
      null
    }
  }

  def template(): Template = {
    var result: Template = null
    val resetPosition = input.offset
    val templDecl = templateDeclaration()
    if (templDecl != null) {
       anyUntil(c => c != ' ' && c != '\t', inclusive = false)
      if (check("=")) {
        anyUntil(c => c != ' ' && c != '\t', inclusive = false)
        if (check("{")) {
          val (imports, localDefs, templates, mixeds) = templateContent()
          if (check("}"))
            result = Template(templDecl._1, None, None, templDecl._2, Nil, imports, localDefs, templates, mixeds)
        }
      }
    }

    if (result == null)
      input.regressTo(resetPosition)
    result
  }

  def templateDeclaration(): (PosString, PosString) = {
    if (check("@")) {
      val namepos = input.offset
      val name = identifier() match {
        case null => null
        case id => position(PosString(id), namepos)
      }

      if (name != null) {
        val paramspos = input.offset
        val types = Option(squareBrackets) getOrElse PosString("")
        val args = several[String, ArrayBuffer[String]](parentheses)
        val params = position(PosString(types + args.mkString), paramspos)
        if (params != null)
          return (name, params)
      } else input.regress(1) // don't consume @
    }

    null
  }

  def templateContent(): (Seq[Simple], Seq[Def], Seq[Template], Seq[TemplateTree]) = {
    val imports = new ArrayBuffer[Simple]
    val localDefs = new ArrayBuffer[Def]
    val templates = new ArrayBuffer[Template]
    val mixeds = new ArrayBuffer[TemplateTree]

    var done = false
    while (!done) {
      val impExp = importExpression()
      if (impExp != null) imports += impExp
      else {
        val ldef = localDef()
        if (ldef != null) localDefs += ldef
        else {
          val templ = template()
          if (templ != null) templates += templ
          else {
            val mix = mixed()
            if (mix != null) mixeds ++= mix
            else {
              // check for an invalid '@' symbol, and just skip it so we can continue the parse
              val pos = input.offset
              if (check("@")) error("Invalid '@' symbol", pos)
              else done = true
            }
          }
        }
      }
    }

    (imports, localDefs, templates, mixeds)
  }

  def extraImports(): Seq[Simple] = {
    var resetPosition = input.offset
    val imports = new ArrayBuffer[Simple]

    lastComment()

    var done = false
    while (!done) {
      val importExp = importExpression()
      if (importExp ne null) {
        imports += importExp
        resetPosition = input.offset
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
    val result = several[String, ArrayBuffer[String]](parentheses)
    if (result.length > 0)
      result.mkString
    else
      null
  }

  /**
    * Parse the template arguments, if they exist
    */
  private def maybeTemplateArgs(): Option[PosString] = {
    if (check("@(")) {
      input.regress(1)
      val p = input.offset
      val args = templateArgs()
      if (args != null) Some(position(PosString(args), p))
      else None
    } else None
  }

  /**
    * Parse the template arguments, if they exist
    */
  private def constructorArgs(): PosString = {
    if (check("@this(")) {
      input.regress(1)
      val p = input.offset
      val args = templateArgs()
      if (args != null) position(PosString(args), p)
      else null
    } else null
  }

  def parse(source: String): ParseResult = {
    // Initialize mutable state
    input.reset(source)
    errorStack.clear()

    val topImports = extraImports()
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
    val args = maybeTemplateArgs()
    val (imports, localDefs, templates, mixeds) = templateContent()

    val template = Template(PosString(""), constructor, argsComment, args.getOrElse(PosString("()")), topImports, imports, localDefs, templates, mixeds)

    if (errorStack.length == 0)
      Success(template, input)
    else
      Error(template, input, errorStack.toList)

  }

  def mkRegressionStatisticsString() {
    val a = input.regressionStatistics.toArray.sortBy { case (m, (c, a)) => c }
    a.mkString("\n")
  }

  // TODO - only for debugging purposes, remove before release
  def setSource(source: String) {
    input.reset(source)
  }
}
