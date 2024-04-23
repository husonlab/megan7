package megan.main;

import jloda.fx.util.ResourceManagerFX;
import jloda.phylo.NewickIO;
import jloda.swing.util.ResourceManager;
import jloda.util.ProgramProperties;

/**
 * setup code
 * Daniel Huson. 2.2024
 */
public class Setup {
    /**
     * setup internal setuff
     */
    public static void apply() {
        NewickIO.NUMBERS_ON_INTERNAL_NODES_ARE_CONFIDENCE_VALUES = false;
        ResourceManager.insertResourceRoot(jloda.resources.Resources.class);
        ResourceManager.insertResourceRoot(megan.resources.Resources.class);
        ResourceManagerFX.addResourceRoot(Megan7.class, "megan.resources");
        ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);
    }
}
