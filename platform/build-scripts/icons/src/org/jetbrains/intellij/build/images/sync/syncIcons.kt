// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.intellij.build.images.sync

import java.io.File

internal fun syncIcons(context: Context) {
  val devIcons = context.devIcons
  val icons = context.icons
  if (context.doSyncIconsRepo || context.doSyncIconsAndCreateReview) {
    log("Syncing icons repo:")
    syncAdded(context.addedByDev, devIcons, context.iconsRepoDir) { context.iconsRepo }
    syncModified(context.modifiedByDev, icons, devIcons)
    syncRemoved(context.removedByDev, icons)
  }
  if (context.doSyncDevRepo || context.doSyncDevIconsAndCreateReview) {
    log("Syncing dev repo:")
    syncAdded(context.addedByDesigners, icons, context.devRepoDir) { changesToReposMap(it) }
    syncModified(context.modifiedByDesigners, devIcons, icons)
    if (context.doSyncRemovedIconsInDev) syncRemoved(context.removedByDesigners, devIcons)
  }
}

internal fun syncAdded(added: Collection<String>,
                       sourceRepoMap: Map<String, GitObject>,
                       targetDir: File, targetRepo: (File) -> File) {
  callSafely {
    stageFiles { add ->
      added.forEach {
        val target = targetDir.resolve(it)
        if (target.exists()) log("$it already exists in target repo!")
        val source = sourceRepoMap[it]!!.file
        source.copyTo(target, overwrite = true)
        val repo = targetRepo(target)
        add(repo, target.relativeTo(repo).path)
      }
    }
  }
}

internal fun syncModified(modified: Collection<String>,
                          targetRepoMap: Map<String, GitObject>,
                          sourceRepoMap: Map<String, GitObject>) {
  callSafely {
    stageFiles { add ->
      modified.forEach {
        val target = targetRepoMap[it]!!
        val source = sourceRepoMap[it]!!
        source.file.copyTo(target.file, overwrite = true)
        add(target.repo, target.path)
      }
    }
  }
}

internal fun syncRemoved(removed: Collection<String>,
                         targetRepoMap: Map<String, GitObject>) {
  callSafely {
    stageFiles { add ->
      removed.map { targetRepoMap[it]!! }.forEach { it ->
        val target = it.file
        if (!target.delete()) {
          log("Failed to delete ${target.absolutePath}")
        }
        else {
          add(it.repo, it.path)
          if (target.parentFile.list().isEmpty()) target.parentFile.delete()
        }
      }
    }
  }
}

private fun stageFiles(action: ((File, String) -> Unit) -> Unit) {
  val map = mutableMapOf<File, MutableList<String>>()
  action { repo, path ->
    if (!map.containsKey(repo)) map[repo] = mutableListOf()
    map[repo]!!.add(path)
  }
  map.forEach { repo, file ->
    stageFiles(file, repo)
  }
}