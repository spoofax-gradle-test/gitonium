package mb.gitonium

import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.IOException
import java.util.regex.Pattern

@Suppress("unused")
open class GitoniumExtension(private val project: Project) {
  var tagPattern: Pattern = Pattern.compile(".*release-(.+)")
  var autoSetVersion: Boolean = true
  var autoSetSubprojectVersions: Boolean = true

  val version: String by lazy {
    val repo = try {
      FileRepositoryBuilder().readEnvironment().findGitDir(project.rootDir).setMustExist(true).build()
    } catch(e: RepositoryNotFoundException) {
      throw GradleException("Gitonium cannot set project version; no git repository found at ${project.rootDir}", e)
    }

    val head = try {
      repo.resolve(Constants.HEAD)
        ?: throw GradleException("Gitonium cannot set project version; repository has no HEAD")
    } catch(e: IOException) {
      throw GradleException("Gitonium cannot set project version; exception occurred when resolving repository HEAD", e)
    }

    val releaseVersion = releaseVersionFromTag(repo, head, tagPattern)

    val branch = run {
      val headRef = repo.exactRef(Constants.HEAD)
        ?: throw GradleException("Gitonium cannot set project version; repository has no HEAD")
      if(headRef.isSymbolic) {
        Repository.shortenRefName(headRef.target.name)
      } else {
        null
      }
    }

    when {
      releaseVersion != null -> releaseVersion
      branch != null -> "$branch-SNAPSHOT"
      else -> throw GradleException("Gitonium cannot set project version; repository HEAD is detached (and does not have a release tag)")
    }
  }

  private fun releaseVersionFromTag(repo: Repository, head: ObjectId, tagPattern: Pattern): String? {
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

class LazyGitoniumVersion(private val project: Project, private val extension: GitoniumExtension, private val isSubProject: Boolean) {
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
    val extension = GitoniumExtension(project)
    project.extensions.add("gitonium", extension)
    project.version = LazyGitoniumVersion(project, extension, false)
    project.subprojects.forEach {
      it.version = LazyGitoniumVersion(project, extension, true)
    }
  }
}
