package forward.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
        name = "test-compile-forward",
        defaultPhase = LifecyclePhase.TEST_COMPILE,
        requiresDependencyCollection = ResolutionScope.TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class TestCompileForwardMojo extends BaseCompileForwardMojo {

    @Override
    public void execute() throws MojoExecutionException {
        compileForward(true);
    }
}
