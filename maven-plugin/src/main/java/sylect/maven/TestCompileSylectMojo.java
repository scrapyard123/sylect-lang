package sylect.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
        name = "test-compile-sylect",
        defaultPhase = LifecyclePhase.TEST_COMPILE,
        requiresDependencyCollection = ResolutionScope.TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class TestCompileSylectMojo extends BaseCompileSylectMojo {

    @Override
    public void execute() throws MojoExecutionException {
        compileSylect(true);
    }
}
