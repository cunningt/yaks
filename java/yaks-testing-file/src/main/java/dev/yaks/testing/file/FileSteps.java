package dev.yaks.testing.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.consol.citrus.Citrus;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.dsl.runner.TestRunner;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.cucumber.datatable.DataTable;

/**
 * @author Tom Cunningham
 * 
 **/
 
public class FileSteps {

    @CitrusResource
    private TestRunner runner;

    @CitrusFramework
    private Citrus citrus;

    private File file;

    @Before
    public void before(Scenario scenario) {
    }

    @Given("^(?:F|f)ile$")
    public void setFile(DataTable properties) {
        Map<String, String> connectionProps = properties.asMap(String.class, String.class);
        String filename = connectionProps.getOrDefault("filename", "");
        file = new File(filename);
    }

    @Then("^verify exist$")
    public void verifyExist() {
        if (!file.exists()) {
            throw new CitrusRuntimeException ("File " + file + " does not exist");
        }
    }

    @Then("^verify read$")
    public void verifyRead() {
        if (!file.canRead()) {
            throw new CitrusRuntimeException("File " + file + " cannot be read");
        }
    }

    @Then("^verify write")
    public void verifyWrite() {
        if (!file.canWrite()) {
            throw new CitrusRuntimeException("File " + file + " cannot be written");
        }
    }

}
