/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.openapi.vcs.changes.committed;

import com.intellij.CommonBundle;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vcs.VcsBundle;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList;
import com.intellij.util.AsynchConsumer;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author max
 */
public class ChangesBrowserDialog extends DialogWrapper {
  private Project myProject;
  private CommittedChangesTableModel myChanges;
  private Mode myMode;
  private CommittedChangesBrowser myCommittedChangesBrowser;
  private AsynchConsumer<List<CommittedChangeList>> myAppender;

  public enum Mode { Simple, Browse, Choose }

  public ChangesBrowserDialog(Project project, CommittedChangesTableModel changes, final Mode mode) {
    super(project, true);
    initDialog(project, changes, mode);
  }

  public ChangesBrowserDialog(Project project, Component parent, CommittedChangesTableModel changes, final Mode mode) {
    super(parent, true);
    initDialog(project, changes, mode);
  }

  private void initDialog(final Project project, final CommittedChangesTableModel changes, final Mode mode) {
    myProject = project;
    myChanges = changes;
    myMode = mode;
    setTitle(VcsBundle.message("dialog.title.changes.browser"));
    setCancelButtonText(CommonBundle.getCloseButtonText());
    final ModalityState currentState = ModalityState.current();
    if ((mode != Mode.Choose) && (ModalityState.NON_MODAL.equals(currentState))) {
      setModal(false);
    }
    myAppender = new AsynchConsumer<List<CommittedChangeList>>() {

      public void finished() {
        new AbstractCalledLater(myProject, ModalityState.stateForComponent(myCommittedChangesBrowser)) {
          public void run() {
            myCommittedChangesBrowser.stopLoading();
          }
        }.callMe();
      }

      public void consume(final List<CommittedChangeList> committedChangeLists) {
        new AbstractCalledLater(myProject, ModalityState.stateForComponent(myCommittedChangesBrowser)) {
          public void run() {
            final boolean selectFirst = (myChanges.getRowCount() == 0) && (!committedChangeLists.isEmpty());
            myChanges.addRows(committedChangeLists);
            if (selectFirst) {
              myCommittedChangesBrowser.selectFirstIfAny();
            } else {
              myCommittedChangesBrowser.resortKeepSelection();
            }
          }
        }.callMe();
      }
    };

    init();
  }

  public AsynchConsumer<List<CommittedChangeList>> getAppender() {
    return myAppender;
  }

  public void startLoading() {
    myCommittedChangesBrowser.startLoading();
  }

  protected String getDimensionServiceKey() {
    return "VCS.ChangesBrowserDialog";
  }

  protected JComponent createCenterPanel() {
    myCommittedChangesBrowser = new CommittedChangesBrowser(myProject, myChanges);
    return myCommittedChangesBrowser;
  }

  @Override
  protected void dispose() {
    super.dispose();
    myCommittedChangesBrowser.dispose();
  }

  @Override
  protected void createDefaultActions() {
    super.createDefaultActions();
    if (myMode == Mode.Browse) {
      getOKAction().putValue(Action.NAME, VcsBundle.message("button.search.again"));
    }
  }

  @Override
  protected Action[] createActions() {
    if (myMode == Mode.Simple) {
      return new Action[] { getCancelAction() };
    }
    return super.createActions();
  }

  public CommittedChangeList getSelectedChangeList() {
    return myCommittedChangesBrowser.getSelectedChangeList();
  }
}
