/*******************************************************************************
* Copyright (c) 2010-2013 ITER Organization.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/
package org.csstudio.autocomplete.ui;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;

public class AutoCompleteTextCellEditor extends TextCellEditor {

	private ContentProposalAdapter contentProposalAdapter;
	private boolean popupOpen = false; // true, if popup is currently open

	public AutoCompleteTextCellEditor(Composite parent, String type) {
		super(parent);

		AutoCompleteProposalProvider provider = new AutoCompleteProposalProvider(
				type);
		contentProposalAdapter = new ContentProposalAdapter(text,
				new TextContentAdapter(), provider,
				AutoCompleteWidget.getActivationKeystroke(),
				AutoCompleteWidget.getAutoactivationChars());
		contentProposalAdapter
				.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		enableContentProposal(provider,
				AutoCompleteWidget.getActivationKeystroke(),
				AutoCompleteWidget.getAutoactivationChars());
	}

	private void enableContentProposal(AutoCompleteProposalProvider provider,
			KeyStroke keyStroke, char[] autoActivationCharacters) {
		// Listen for popup open/close events to be able to handle focus events
		// correctly
		contentProposalAdapter
				.addContentProposalListener(new IContentProposalListener2() {

					public void proposalPopupClosed(
							ContentProposalAdapter adapter) {
						popupOpen = false;
					}

					public void proposalPopupOpened(
							ContentProposalAdapter adapter) {
						popupOpen = true;
					}
				});
	}

	/**
	 * Return the {@link ContentProposalAdapter} of this cell editor.
	 * 
	 * @return the {@link ContentProposalAdapter}
	 */
	public ContentProposalAdapter getContentProposalAdapter() {
		return contentProposalAdapter;
	}

	@Override
	protected void focusLost() {
		if (!popupOpen) {
			// Focus lost deactivates the cell editor.
			// This must not happen if focus lost was caused by activating
			// the completion proposal popup.
			super.focusLost();
		}
	}

	@Override
	protected boolean dependsOnExternalFocusListener() {
		// Always return false;
		// Otherwise, the ColumnViewerEditor will install an additional focus
		// listener
		// that cancels cell editing on focus lost, even if focus gets lost due
		// to
		// activation of the completion proposal popup. See also bug 58777.
		return false;
	}
}