/*
 * Copyright (c) 2006 Stiftung Deutsches Elektronen-Synchrotron,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS.
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE.
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS,
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION,
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */
/*
 * $Id$
 */
package org.csstudio.config.ioconfig.config.view;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.csstudio.config.ioconfig.config.view.helper.ConfigHelper;
import org.csstudio.config.ioconfig.model.Document;
import org.csstudio.config.ioconfig.model.Node;
import org.csstudio.config.ioconfig.model.Repository;
import org.csstudio.config.ioconfig.model.Sensors;
import org.csstudio.config.ioconfig.model.pbmodel.Channel;
import org.csstudio.config.ioconfig.model.pbmodel.ChannelStructure;
import org.csstudio.config.ioconfig.model.pbmodel.GSDFile;
import org.csstudio.config.ioconfig.model.pbmodel.GSDModule;
import org.csstudio.config.ioconfig.model.pbmodel.Module;
import org.csstudio.config.ioconfig.model.pbmodel.ModuleChannelPrototype;
import org.csstudio.config.ioconfig.view.Activator;
import org.csstudio.config.ioconfig.view.ProfiBusTreeView;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author hrickens
 * @author $Author$
 * @version $Revision$
 * @since 06.06.2007
 */
public class ChannelConfigComposite extends NodeConfig {

    /**
     * The Profibus Channel Object.
     */
    private Channel _channel;
    /**
     * The EPICS address string of the Channel.
     */
    private Text _addressText;
    /**
     * The GSD File of the parent Module.
     */
    private GSDFile _gsdFile;
    private Text _ioNameText;
    private ComboViewer _sensorsViewer;

    /**
     * Open a channel configuration view for a exist {@link Channel}.
     * 
     * @param parent
     *            The parent Composite of this Configuration View
     * @param profiBusTreeView
     *            The TreeViewer.
     * 
     * @param channel
     *            the channel to configured.
     */
    public ChannelConfigComposite(final Composite parent, final ProfiBusTreeView profiBusTreeView,
            final Channel channel) {
        super(parent, profiBusTreeView, "Profibus Channel Configuration", channel, channel == null);
        _channel = channel;
        if (_channel == null) {
            newNode();
        } else {
            _gsdFile = _channel.getGSDFile();
        }
        setSavebuttonEnabled(null, getNode().isPersistent());
        String[] heads = { "Channel settings", "Documents", "GSD File List" };
        general(heads[0]);
        documents();
        if (_gsdFile != null) {
            fill(_gsdFile);
        }
        getTabFolder().pack();
    }

    /**
     * @param head
     *            is TabHead Text
     */
    private void general(final String head) {
        final Composite comp = ConfigHelper.getNewTabItem(head, getTabFolder(), 5,300,290);
        comp.setLayout(new GridLayout(4, false));

        // Name \ Index Group
        Group gName = new Group(comp, SWT.NONE);
        gName.setText("Name");
        gName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
        gName.setLayout(new GridLayout(3, false));
        setNameWidget(new Text(gName, SWT.BORDER | SWT.SINGLE));
        getNameWidget().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
        setText(getNameWidget(), _channel.getName(), 255);
        getNameWidget().setEditable(false);
        
        setIndexSpinner(ConfigHelper.getIndexSpinner(gName, _channel, getMLSB(), "Index",
                getProfiBusTreeView()));
        getIndexSpinner().setEnabled(false);

        // IO Name Group
        Group ioNameGroup = new Group(comp, SWT.NONE);
        ioNameGroup.setLayout(new GridLayout(1, false));
        ioNameGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        ioNameGroup.setText("IO Name: ");
        _ioNameText = new Text(ioNameGroup, SWT.BORDER | SWT.SINGLE);
        _ioNameText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        setText(_ioNameText, _channel.getIoName(), 255);
        
        List<Sensors> loadSensors = Repository.loadSensors(_channel.getIoName());
        if((loadSensors==null||loadSensors.size()<1)) {
            new Label(ioNameGroup, SWT.NONE);
        }else {
            Group sensorsGroup = new Group(comp, SWT.NONE);
            sensorsGroup.setLayout(new GridLayout(1, false));
            sensorsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
            sensorsGroup.setText("Sensors: ");
            _sensorsViewer = new ComboViewer(sensorsGroup, SWT.READ_ONLY);
            _sensorsViewer.setLabelProvider(new LabelProvider());
            _sensorsViewer.setContentProvider(new ArrayContentProvider());
            _sensorsViewer.setInput(loadSensors.toArray());
            int id = 0;
            if(_channel.getCurrentValue()!=null&&_channel.getCurrentValue().length()>0) {
                id = Integer.parseInt(_channel.getCurrentValue());
            }
            _sensorsViewer.getCombo().select(0);
            for (Sensors sensors : loadSensors) {
                if(id==sensors.getId()) {
                    _sensorsViewer.setSelection(new StructuredSelection(sensors));
                }
            }
            _sensorsViewer.getCombo().setData(_sensorsViewer.getCombo().getSelectionIndex());
            _sensorsViewer.getCombo().addModifyListener(getMLSB());
        }

        // EPICS address Group
        Group epicsAddressGroup = new Group(comp, SWT.NONE);
        epicsAddressGroup.setLayout(new GridLayout(2, false));
        epicsAddressGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        epicsAddressGroup.setText("EPICS address string: ");

        _addressText = new Text(epicsAddressGroup, SWT.FLAT | SWT.SINGLE);
        if (_channel.getName() != null) {
            _addressText.setText(_channel.getEpicsAddressStringNH());
        }
        _addressText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
        _addressText.setEditable(false);
        _addressText.addModifyListener(new ModifyListener() {

            public void modifyText(final ModifyEvent e) {
                setSavebuttonEnabled("epicsAddressString", true);
            }
        });
        Button assembleButton = new Button(epicsAddressGroup, SWT.FLAT);
        assembleButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        assembleButton.setImage(Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/refresh.gif").createImage());
        assembleButton.setToolTipText("Refresh the EPICS Address String\n and save it into the DB");
        assembleButton.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
                doAssemble();
            }

            public void widgetSelected(SelectionEvent e) {
                doAssemble();
            }

            private void doAssemble() {
                GSDModule module = _channel.getModule().getGSDModule();
                TreeSet<ModuleChannelPrototype> moduleChannelPrototypes = module
                        .getModuleChannelPrototypeNH();
                ModuleChannelPrototype[] array = moduleChannelPrototypes
                        .toArray(new ModuleChannelPrototype[0]);
                ModuleChannelPrototype moduleChannelPrototype = array[_channel.getChannelStructure().getSortIndex()];
                _channel.setStatusAddressOffset(moduleChannelPrototype.getShift());
                String name;
                if (!moduleChannelPrototype.isStructure()) {
                    name = moduleChannelPrototype.getName();
                } else {
                    name = moduleChannelPrototype.getName() + _channel.getSortIndex();
                }
                if (!name.equals(_channel.getName())) {
                    getNameWidget().setText(name);
                }
                if (moduleChannelPrototype.getType() != _channel.getChannelStructure()
                        .getStructureType()) {
                    _channel.getChannelStructure().setStructureType(
                            moduleChannelPrototype.getType());
                    if (_channel.getChannelStructure().isSimple()) {
                        _channel.setChannelType(moduleChannelPrototype.getType());
                    }
                }
                if (!_channel.getName().equals(moduleChannelPrototype.getName())) {
                    _channel.setName(moduleChannelPrototype.getName());
                }
                String oldAdr = _channel.getEpicsAddressStringNH();
                _channel.assembleEpicsAddressString();
                String newAdr = _channel.getEpicsAddressStringNH();
                if (!newAdr.equals(oldAdr)) {
                    _addressText.setText(newAdr);
                }
            }
        });

        // Size
        Group sizeGroup = new Group(comp, SWT.NONE);
        sizeGroup.setLayout(new GridLayout(1, false));
        sizeGroup.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 1, 1));
        sizeGroup.setText("Size: ");
        Text sizeText = new Text(sizeGroup, SWT.SINGLE | SWT.RIGHT);
        sizeText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        setText(sizeText, _channel.getChSize(), 255);
        sizeText.setEditable(false);

        // Description Group
        makeDescGroup(comp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean fill(final GSDFile gsdFile) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final GSDFile getGSDFile() {
        return null;
    }

    /**
     * Store all Data in {@link Channel} DB object.
     */
    public final void store() {
        super.store();
        // Channel Settings
        _channel.setIoName(_ioNameText.getText());
        _channel.setName(getNameWidget().getText());
        _ioNameText.setData(_ioNameText.getText());
        if(_sensorsViewer!=null) {
            Sensors firstElement = (Sensors) ((StructuredSelection)_sensorsViewer.getSelection()).getFirstElement();
            _channel.setCurrentValue(Integer.toString(firstElement.getId()));
            Combo combo = _sensorsViewer.getCombo();
            combo.setData(combo.getSelectionIndex());
        }
        // Document
        Set<Document> docs = getDocumentationManageView().getDocuments();
        _channel.setDocuments(docs);

        // GSD File
        // GSD File daten brauchen nicht noch mal gesetzt werden. Das geschiet
        // schon beim einlesen des GSD Files.
        save();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Node getNode() {
        if (_channel == null) {
            StructuredSelection selection = (StructuredSelection) getProfiBusTreeView().getTreeViewer()
                    .getSelection();
            if (selection.getFirstElement() instanceof Module) {
                Module module = (Module) selection.getFirstElement();
                _channel = ChannelStructure.makeSimpleChannel(module, false).getFirstChannel();
            }

        }
        return _channel;
    }

    /**
     * Cancel all change value.
     */
    @Override
    public final void cancel() {
        super.cancel();
        getIndexSpinner().setSelection((Short) getIndexSpinner().getData());
        setName((String) getNameWidget().getData());
        if (_channel != null) {
            _gsdFile = null;
            if (_channel.getGSDFile() != null) {
                _gsdFile = _channel.getGSDFile();
                fill(_gsdFile);

            } else {
                ((Text) ChannelConfigComposite.this.getData("version")).setText("");
            }
        } else {
            _gsdFile = null;
            fill(_gsdFile);
        }
    }
    
    /**
     * 
     * @param ioNameText Set the new IOName for this channel. 
     */
    public void setIoNameText(String ioNameText) {
		_ioNameText.setText(ioNameText);
	}
}
