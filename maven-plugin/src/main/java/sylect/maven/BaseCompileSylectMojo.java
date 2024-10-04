// SPDX-License-Identifier: MIT

package sylect.maven;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import sylect.SylectCompilerRunner;
import sylect.CompilationException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

abstract public class BaseCompileSylectMojo extends AbstractMojo {

    @Parameter(required = true, readonly = true, property = "project")
    protected MavenProject project;

    @Parameter(required = true, readonly = true, property = "sylect.target")
    protected int target;

    protected void compileSylect(boolean tests) throws MojoExecutionException {
        var classPath = new ArrayList<String>();
        try {
            if (tests) {
                classPath.addAll(project.getTestClasspathElements());
            } else {
                classPath.addAll(project.getCompileClasspathElements());
                classPath.addAll(project.getRuntimeClasspathElements());
            }
        } catch (DependencyResolutionRequiredException e) {
            throw new RuntimeException(e);
        }
        var classLoader = createCompilerClassLoader(classPath);

        var sourceRoots = tests ?
                project.getTestCompileSourceRoots() :
                project.getCompileSourceRoots();

        var outputDir = tests ?
                project.getBuild().getTestOutputDirectory() :
                project.getBuild().getOutputDirectory();

        try {
            SylectCompilerRunner.compileSourceTrees(
                    classLoader,
                    target,
                    sourceRoots.stream()
                            .map(Paths::get)
                            .map(Path::toAbsolutePath)
                            .toList(),
                    Paths.get(outputDir).toAbsolutePath(),
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
