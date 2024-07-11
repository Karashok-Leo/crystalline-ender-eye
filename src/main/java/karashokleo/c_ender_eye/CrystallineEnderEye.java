package karashokleo.c_ender_eye;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class CrystallineEnderEye implements ModInitializer
{
    public static CrystallineEnderEyeConfig config;

    @Override
    public void onInitialize()
    {
        AutoConfig.register(CrystallineEnderEyeConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(CrystallineEnderEyeConfig.class).getConfig();
        CrystallineEnderEyeItem item = Registry.register(Registries.ITEM, "c_ender_eye:c_ender_eye", new CrystallineEnderEyeItem());
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(item.getDefaultStack()));
        AttackEntityCallback.EVENT.register(CrystallineEnderEyeItem::attack);
    }
}