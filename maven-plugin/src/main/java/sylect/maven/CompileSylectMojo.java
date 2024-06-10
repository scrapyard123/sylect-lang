package sylect.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
        name = "compile-sylect",
        defaultPhase = LifecyclePhase.COMPILE,
        requiresDependencyCollection = ResolutionScope.RUNTIME_PLUS_SYSTEM,
        requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM)
public class CompileSylectMojo extends BaseCompileSylectMojo {

    @Override
    public void execute() throws MojoExecutionException {
        compileSylect(false);
    }
}
