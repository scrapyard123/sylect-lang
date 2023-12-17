package forward.maven;

import forward.bootstrap.BootstrapCompiler;
import forward.bootstrap.CompilationException;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

@Mojo(
        name = "compile-forward",
        defaultPhase = LifecyclePhase.COMPILE,
        requiresDependencyCollection = ResolutionScope.RUNTIME_PLUS_SYSTEM,
        requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM)
public class ForwardMojo extends AbstractMojo {

    @Parameter(required = true, readonly = true, property = "project")
    protected MavenProject project;

    @Parameter(required = true, readonly = true, property = "forward.target")
    protected int target;

    @Override
    public void execute() throws MojoExecutionException {
        var classPath = new ArrayList<String>();
        try {
            classPath.addAll(project.getCompileClasspathElements());
            classPath.addAll(project.getRuntimeClasspathElements());
            // TODO: Add missing features to support running tests
            // classPath.addAll(project.getTestClasspathElements());
        } catch (DependencyResolutionRequiredException e) {
            throw new RuntimeException(e);
        }

        var classLoader = createCompilerClassLoader(classPath);

        var sourceRoots = new ArrayList<String>();
        sourceRoots.addAll(project.getCompileSourceRoots());
        // TODO: Add missing features to support running tests
        // sourceRoots.addAll(project.getTestCompileSourceRoots());

        try {
            BootstrapCompiler.compileSourceTrees(
                    classLoader,
                    target,
                    sourceRoots.stream().map(Paths::get).map(Path::toAbsolutePath).toList(),
                    Paths.get(project.getBuild().getOutputDirectory()).toAbsolutePath(),
                    getLog()::info);
        } catch (CompilationException e) {
            throw new MojoExecutionException("failed to compile", e);
        }
    }

    private ClassLoader createCompilerClassLoader(Collection<String> classPath) throws MojoExecutionException {
        var uris = classPath.stream()
                .map(Paths::get)
                .map(Path::toUri)
                .toArray(URI[]::new);

        var urls = new URL[uris.length];
        for (int i = 0; i < uris.length; i++) {
            try {
                urls[i] = uris[i].toURL();
            } catch (MalformedURLException e) {
                throw new MojoExecutionException("failed to build classpath", e);
            }
        }

        return new URLClassLoader(urls, ClassLoader.getSystemClassLoader());
    }
}
