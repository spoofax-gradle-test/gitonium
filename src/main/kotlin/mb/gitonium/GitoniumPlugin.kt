package mb.gitonium

import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import java.util.regex.Pattern

@Suppress("unused")
open class GitoniumExtension {
  var mainBranch: String = "master"
  var tagPattern: Pattern = Pattern.compile("release-(.+)")
  var setSubprojectVersions: Boolean = true
}

@Suppress("unused")
class GitoniumPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.extensions.add("gitonium", GitoniumExtension())
    project.afterEvaluate { configure(project) }
  }

  private fun configure(project: Project) {
    val extension = project.extensions.getByType<GitoniumExtension>()
    val repo = FileRepositoryBuilder().readEnvironment().findGitDir(project.rootDir).setMustExist(true).build()
    val head = repo.resolve(Constants.HEAD) ?: throw RuntimeException("Repository has no HEAD")
    val releaseVersion = releaseVersionFromTag(repo, head, extension.tagPattern)
    val branch = repo.branch
    val version = if(releaseVersion != null) {
      if(branch == extension.mainBranch) {
        releaseVersion
      } else {
        "$releaseVersion-$branch"
      }
    } else {
      "$branch-SNAPSHOT"
    }
    project.version = version
    if(extension.setSubprojectVersions) {
      project.subprojects.forEach {
        it.version = version
      }
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