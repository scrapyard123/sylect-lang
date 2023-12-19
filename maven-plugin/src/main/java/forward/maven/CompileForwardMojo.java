package forward.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
        name = "compile-forward",
        defaultPhase = LifecyclePhase.COMPILE,
        requiresDependencyCollection = ResolutionScope.RUNTIME_PLUS_SYSTEM,
        requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM)
public class CompileForwardMojo extends BaseCompileForwardMojo {

    @Override
    public void execute() throws MojoExecutionException {
        compileForward(false);
    }
}
