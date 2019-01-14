package mb.gitonium

import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.*
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import java.io.IOException
import java.util.regex.Pattern

@Suppress("unused")
open class GitoniumExtension(private val project: Project) {
  var tagPattern: Pattern = Pattern.compile(""".*release-(.+)""")
  var autoSetVersion: Boolean = true
  var autoSetSubprojectVersions: Boolean = true


  val version: String by lazy {
    val branch = run {
      val headRef = repo.exactRef(Constants.HEAD)
        ?: throw GradleException("Gitonium cannot set project version; repository has no HEAD")
      if(headRef.isSymbolic) {
        Repository.shortenRefName(headRef.target.name)
      } else {
        null
      }
    }
    val releaseTagVersion = releaseTagVersion // Assign to local val to enable smart cast.
    when {
      releaseTagVersion != null -> releaseTagVersion
      branch != null -> "$branch-SNAPSHOT"
      else -> throw GradleException("Gitonium cannot set project version; repository HEAD is detached (and does not have a release tag)")
    }
  }


  val releaseTagVersion: String? by lazy {
    val head = try {
      repo.resolve(Constants.HEAD)
        ?: throw GradleException("Gitonium cannot set project version; repository has no HEAD")
    } catch(e: IOException) {
      throw GradleException("Gitonium cannot set project version; exception occurred when resolving repository HEAD", e)
    }
    releaseTagVersion(repo, head, tagPattern)
  }

  val isRelease: Boolean get() = releaseTagVersion != null


  private val repo: Repository by lazy {
    try {
      FileRepositoryBuilder().readEnvironment().findGitDir(project.rootDir).setMustExist(true).build()
    } catch(e: RepositoryNotFoundException) {
      throw GradleException("Gitonium cannot set project version; no git repository found at ${project.rootDir}", e)
    }
  }

  private fun releaseTagVersion(repo: Repository, head: ObjectId, tagPattern: Pattern): String? {
    repo.refDatabase.getRefsByPrefix(Constants.R_TAGS).forEach { tagRef ->
      // Peel the ref if it has not been peeled yet, otherwise peeledObjectId will return null.
      val finalTagRef = if(tagRef.isPeeled) {
        tagRef
      } else {
        repo.refDatabase.peel(tagRef)
      }
      val target = finalTagRef.peeledObjectId
      if(target != null && AnyObjectId.equals(head, target)) {
        // Tag names contain 'refs/tags/', which must be removed before matching.
        val name = finalTagRef.name.replace(Constants.R_TAGS, "")
        val matcher = tagPattern.matcher(name)
        if(matcher.matches()) {
          val tagVersion = matcher.group(1)
          if(tagVersion != null) {
            return tagVersion
          }
        }
      }
    }
    return null
  }
}

class LazyGitoniumVersion(private val extension: GitoniumExtension, private val isSubProject: Boolean) {
  override fun toString(): String {
    return when {
      extension.autoSetVersion && !isSubProject -> extension.version
      extension.autoSetSubprojectVersions && isSubProject -> extension.version
      else -> Project.DEFAULT_VERSION
    }
  }
}

@Suppress("unused")
class GitoniumPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    // Create and add extension.
    val extension = GitoniumExtension(project)
    project.extensions.add("gitonium", extension)

    // Set project and sub-project versions
    project.version = LazyGitoniumVersion(extension, false)
    project.subprojects.forEach {
      it.version = LazyGitoniumVersion(extension, true)
    }

    // Register "check for snapshot dependencies" task when publishing for project and sub-projects.
    project.afterEvaluate {
      registerCheckForSnapshotDependenciesTasks(this, extension)
      subprojects.forEach {
        registerCheckForSnapshotDependenciesTasks(it, extension)
      }
    }
  }

  private fun registerCheckForSnapshotDependenciesTasks(project: Project, extension: GitoniumExtension) {
    val publishTask = project.tasks.findByName("publish")
    if(publishTask != null) {
      val checkTask = project.tasks.register<CheckForSnapshotDependencies>("checkReleaseDependenciesSnapshotVersions", extension)
      publishTask.dependsOn(checkTask)
    }
  }
}

open class CheckForSnapshotDependencies(private val extension: GitoniumExtension) : DefaultTask() {
  @TaskAction
  fun check() {
    // TODO: what are the inputs and outputs of this task, for incrementality?
    if(extension.isRelease) {
      val dependencies = mutableSetOf<Dependency>()
      project.configurations.flatMapTo(dependencies) {
        it.allDependencies
      }
      dependencies.forEach {
        val version = it.version // Assign to local val to enable smart cast.
        if(version != null && version.endsWith("-SNAPSHOT")) {
          throw GradleException("")
        }
      }
    }
  }
}
