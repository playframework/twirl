/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package play.twirl.gradle;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

/**
 * A simple unit test for the 'com.playframework.twirl' plugin.
 */
class TwirlPluginTest {
    @Test void pluginRegistersATask() {
        // Create a test project and apply the plugin
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("com.playframework.twirl");

        // Verify the result
        assertNotNull(project.getTasks().findByName("greeting"));
    }
}