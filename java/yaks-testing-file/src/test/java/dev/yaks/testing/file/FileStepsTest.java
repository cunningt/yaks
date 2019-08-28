package dev.yaks.testing.file;

import java.util.function.Consumer;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;

/**
 * @author Tom Cunningham
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        strict = true,
        glue = { "com.consol.citrus.cucumber.step.runner.core",
                  "dev.yaks.testing.file" },
        plugin = { "com.consol.citrus.cucumber.CitrusReporter" } )
public class FileStepsTest {

}
