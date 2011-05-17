/**
 *
 */
package org.csstudio.diag.epics.pvtree;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.data.values.ISeverity;
import org.csstudio.data.values.IValue;
import org.csstudio.data.values.ValueUtil;
import org.csstudio.utility.pv.PV;
import org.csstudio.utility.pv.PVFactory;
import org.csstudio.utility.pv.PVListener;
import org.eclipse.swt.widgets.Display;

/** One item in the PV tree model.
 *  <p>
 *  Since an 'item' is a PV, possibly for a record
 *  which has inputs, and those inputs is what we
 *  want to drill down, this class currently includes
 *  almost all the logic behind the tree creation.
 *
 *  @author Kay Kasemir
 */
class PVTreeItem
{
   /** The model to which this whole tree belongs. */
    private final PVTreeModel model;

    /** The parent of this item, or <code>null</code>. */
    private final PVTreeItem parent;

    /** The info provided by the parent or creator ("PV", "INPA", ...) */
    private final String info;

    /** The name of this PV tree item as shown in the tree. */
    private final String pv_name;

    /** The name of the record.
     *  <p>
     *  For example, the 'name' could be 'fred.SEVR', then 'fred'
     *  would be the record name.
     */
    private final String record_name;

    /** The PV used for getting the current value. */
    private PV pv;

    /** Most recent value. */
    private volatile String value = null;

    /** Most recent severity. */
    private volatile ISeverity severity = null;

    private PVListener pv_listener = new PVListener()
    {
        @Override
        public void pvDisconnected(PV pv)
        {
            value = "<disconnected>"; //$NON-NLS-1$
            severity = null;
            updateValue();
        }
        @Override
        public void pvValueUpdate(PV pv)
        {
            try
            {
                IValue pv_value = pv.getValue();
                value = ValueUtil.formatValueAndSeverity(pv_value);
                severity = pv_value.getSeverity();
                updateValue();
            }
            catch (Exception e)
            {
                Plugin.getLogger().log(Level.SEVERE, "PV Listener error" , e); //$NON-NLS-1$
            }
        }
    };

    /** The PV used for getting the record type. */
    private PV type_pv;
    private String type;
    private PVListener type_pv_listener = new PVListener()
    {
        @Override
        public void pvDisconnected(PV pv)
        {
            // NOP
        }
        @Override
        public void pvValueUpdate(PV pv)
        {
            try
            {
                final String type_txt = pv.getValue().format();
                // type should be a text.
                // If it starts with a number, it's probably not an
                // EPICS record type but a simulated PV
                final char first_char = type_txt.charAt(0);
                if (first_char >= 'a' && first_char <= 'z')
                    type = type_txt;
                else
                    type = Messages.UnkownPVType;
                updateType();
            }
            catch (Exception e)
            {
                Plugin.getLogger().log(Level.SEVERE, "PV Type Listener error" , e); //$NON-NLS-1$
            }
        }
    };

    /** Array of fields to read for this record type.
     *  Fields are removed as they are read, so in the end this
     *  array will be empty
     */
    final private ArrayList<String> links_to_read = new ArrayList<String>();

    /** Used to read the links of this pv. */
    private PV link_pv = null;
    private String link_value;
    private PVListener link_pv_listener = new PVListener()
    {
        @Override
        public void pvDisconnected(PV pv)
        {
            // NOP
        }
        @Override
        public void pvValueUpdate(PV pv)
        {
            try
            {
                link_value = pv.getValue().format();
                // The value could be
                // a) a record name followed by "... NPP NMS". Remove that.
                // b) a hardware input/output "@... " or "#...". Keep that.
                if (link_value.length() > 1 &&
                    link_value.charAt(0) != '@' &&
                    link_value.charAt(0) != '#')
                {
                    int i = link_value.indexOf(' ');
                    if (i > 0)
                        link_value = link_value.substring(0, i);
                }
                updateLink();
            }
            catch (Exception e)
            {
                Plugin.getLogger().log(Level.SEVERE, "PV Link Listener error" , e); //$NON-NLS-1$
            }
        }
    };

    /** Tree item children, populated with info from the input links. */
    private ArrayList<PVTreeItem> links = new ArrayList<PVTreeItem>();

    /** Create a new PV tree item.
     *  @param model The model to which this whole tree belongs.
     *  @param parent The parent of this item, or <code>null</code>.
     *  @param info The info provided by the parent or creator ("PV", "INPA", ...)
     *  @param pv_name The name of this PV entry.
     */
    public PVTreeItem(PVTreeModel model,
            PVTreeItem parent,
            String info,
            String pv_name)
    {
        this.model = model;
        this.parent = parent;
        this.info = info;
        this.pv_name = pv_name;
        this.type = null;

        // In case this is "record.field", get the record name.
        final int sep = pv_name.lastIndexOf('.');
        if (sep > 0)
            record_name = pv_name.substring(0, sep);
        else
            record_name = pv_name;

        Plugin.getLogger().log(Level.FINE,
                "New Tree item {0}, record name {1}", //$NON-NLS-1$
                new Object[] { pv_name, record_name});

        // Avoid loops.
        // If the model already contains an entry with this name,
        // we simply display this new item, but we won't
        // follow its input links.
        final PVTreeItem other = model.findPV(pv_name);

        // Now add this one, otherwise the previous call would have found 'this'.
        if (parent != null)
            parent.links.add(this);

        // Is this a link to follow, or just a constant to display?
        // Hardware links "@vme..." or constant numbers "-12.3"
        // cause us to stop here:
        if (pv_name.startsWith("@") || pv_name.matches("^-?[0-9]"))  //$NON-NLS-1$//$NON-NLS-2$
        {
            pv = null;
            return;
        }

        // Try to read the pv
        try
        {
            pv = createPV(pv_name);
            pv.addListener(pv_listener);
            pv.start();
        }
        catch (Exception e)
        {
            Plugin.getLogger().log(Level.SEVERE, "PV creation error" , e); //$NON-NLS-1$
        }
        // Get type from 'other', previously used PV or via CA
        if (other != null)
        {
            type = other.type;
            Plugin.getLogger().fine("Known item, not traversing inputs (again)"); //$NON-NLS-1$
        }
        else
        {
            try
            {
                type_pv = createPV(record_name + ".RTYP"); //$NON-NLS-1$
                type_pv.addListener(type_pv_listener);
                type_pv.start();
            }
            catch (Exception e)
            {
                Plugin.getLogger().log(Level.SEVERE, "PV.RTYP creation error" , e); //$NON-NLS-1$
            }
        }
    }

    /** Dispose this and all child entries. */
    public void dispose()
    {
        for (PVTreeItem item : links)
            item.dispose();
        if (pv != null)
        {
            pv.removeListener(pv_listener);
            pv.stop();
            pv = null;
        }
        disposeLinkPV();
        disposeTypePV();
    }

    /** @return PV for the given name */
    @SuppressWarnings("nls")
    private PV createPV(final String name)
    {
        try
        {
            return PVFactory.createPV(name);
        }
        catch (Exception ex)
        {
            Plugin.getLogger().log(Level.SEVERE, "Cannot create PV '" + name + "'", ex);
        }
        return null;
    }

    /** Delete the type_pv */
    private void disposeTypePV()
    {
        if (type_pv != null)
        {
            type_pv.removeListener(type_pv_listener);
            type_pv.stop();
            type_pv = null;
        }
    }

    /** Delete the link_pv */
    private void disposeLinkPV()
    {
        if (link_pv != null)
        {
            link_pv.removeListener(link_pv_listener);
            link_pv.stop();
            link_pv = null;
        }
    }

    /** @return Returns the name of this PV. */
    public String getPVName()
    {   return pv_name; }

    /** @return Severity of current value. May be <code>null</code>. */
    public ISeverity getSeverity()
    {   return severity; }

    /** @return Returns the record type of this item or <code>null</code>. */
    public String getType()
    {   return type;    }

    /** @return Returns the parent or <code>null</code>. */
    public PVTreeItem getParent()
    {   return parent;    }

    /** @return Returns the first link or <code>null</code>. */
    public PVTreeItem getFirstLink()
    {
        if (links.size() > 0)
            return links.get(0);
        return null;
    }

    /** @return Returns the all links. */
    public PVTreeItem[] getLinks()
    {
        return (PVTreeItem[]) links.toArray(new PVTreeItem[links.size()]);
    }

    /** @return Returns <code>true</code> if this item has any links. */
    public boolean hasLinks()
    {
        return links.size() > 0;
    }

    /** @return Returns a String. No really, it does! */
    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        StringBuffer b = new StringBuffer();
        b.append(info);
        b.append(" '");
        b.append(pv_name);
        b.append("'");
        if (type != null)
        {
            b.append("  (");
            b.append(type);
            b.append(")");
        }
        if (value != null)
        {
            b.append("  =  ");
            b.append(value);
        }
        return b.toString();
    }

    /** Thread-safe handling of the 'value' update. */
    private void updateValue()
    {
        Display.getDefault().asyncExec(new Runnable()
        {
            @Override
            public void run()
            {   // Display the received type of this record.
                model.itemUpdated(PVTreeItem.this);
            }
        });
    }

    /** Thread-safe handling of the 'type' update. */
    @SuppressWarnings("nls")
    private void updateType()
    {
        Plugin.getLogger().log(Level.FINE,
                "{0} received type {1}", new Object[] { pv_name, type });
        Display.getDefault().asyncExec(new Runnable()
        {
            @Override
            public void run()
            {
                // Already disposed?
                if (type_pv == null)
                    return;
                // We got the type, so close the connection.
                disposeTypePV();
                // Display the received type of this record.
                model.itemChanged(PVTreeItem.this);

                links_to_read.clear();
                final List<String> fields = model.getFieldInfo().get(type);
                if (fields != null)
                    links_to_read.addAll(fields);
                if (links_to_read.size() <= 0)
                {
                    Plugin.getLogger().log(Level.FINE,
                            "{0} has unknown record type {1}", new Object[] { pv_name, type });
                    return;
                }
                getNextLink();
            }
        });
    }

    /** Helper for reading next link from links_to_read. */
    @SuppressWarnings("nls")
    private void getNextLink()
    {
        // Probably superflous, but can't hurt
        disposeLinkPV();
        // Any more links to read?
        if (links_to_read.size() <= 0)
            return;
        final String field = links_to_read.get(0);
        final String link_name = record_name + "." + field;
        try
        {
            link_pv = createPV(link_name);
            link_pv.addListener(link_pv_listener);
            link_pv.start();
        }
        catch (Exception e)
        {
            Plugin.getLogger().log(Level.SEVERE, "PV." + field + " creation error" , e); //$NON-NLS-1$
        }
    }

    /** Thread-safe handling of the 'link_pv' update. */
    @SuppressWarnings("nls")
    private void updateLink()
    {
        Plugin.getLogger().log(Level.FINE,
                "{0} received {1}", new Object[] { link_pv.getName(), link_value });

        Display.getDefault().asyncExec(new Runnable()
        {
            @Override
            public void run()
            {
                if (link_pv == null)
                {
                    Plugin.getLogger().log(Level.FINE,
                            "{0} already disposed", pv_name);
                    return;
                }
                disposeLinkPV();

                // Remove field for which we received update from
                // list of links to read
                if (links_to_read.size() <= 0)
                {
                    Plugin.getLogger().log(Level.FINE,
                            "{0} update without active link?",link_pv.getName());
                    return;
                }
                final String field = links_to_read.remove(0);
                if (link_value.length() > 0)
                {
                    new PVTreeItem(model, PVTreeItem.this, field, link_value);
                    model.itemChanged(PVTreeItem.this);
                }
                getNextLink();
            }
        });
    }
}
