package taboolib.module.nms;

import net.minecraft.server.v1_12_R1.EntityVillager;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_13_R2.IRegistry;
import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.*;
import net.minecraft.server.v1_15_R1.LightEngineThreaded;
import net.minecraft.server.v1_16_R1.WorldDataServer;
import net.minecraft.server.v1_8_R3.NBTTagByte;
import net.minecraft.server.v1_8_R3.NBTTagByteArray;
import net.minecraft.server.v1_8_R3.NBTTagDouble;
import net.minecraft.server.v1_8_R3.NBTTagFloat;
import net.minecraft.server.v1_8_R3.NBTTagInt;
import net.minecraft.server.v1_8_R3.NBTTagIntArray;
import net.minecraft.server.v1_8_R3.NBTTagList;
import net.minecraft.server.v1_8_R3.NBTTagLong;
import net.minecraft.server.v1_8_R3.NBTTagShort;
import net.minecraft.server.v1_8_R3.NBTTagString;
import org.bukkit.Chunk;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftVillager;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import taboolib.common.platform.FunctionKt;
import taboolib.common.reflect.Ref;
import taboolib.common.reflect.Reflex;
import taboolib.module.nms.type.LightType;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static taboolib.module.nms.MinecraftServerUtilKt.sendPacket;

/**
 * TabooLib
 * taboolib.module.nms.NMS
 *
 * @author sky
 * @since 2021/6/18 8:54 ??????
 */
@SuppressWarnings("ALL")
public class NMSGenericImpl extends NMSGeneric {

    private Field entityTypesField;

    public NMSGenericImpl() {
        if (taboolib.module.nms.MinecraftVersion.INSTANCE.getMajor() >= 5) {
            for (Field declaredField : net.minecraft.server.v1_12_R1.Entity.class.getDeclaredFields()) {
                if (declaredField.getType().getSimpleName().equals("EntityTypes")) {
                    entityTypesField = declaredField;
                    break;
                }
            }
        }
    }

    @NotNull
    @Override
    public String getKey(ItemStack itemStack) {
        Object nmsItem = CraftItemStack.asNMSCopy(itemStack);
        if (taboolib.module.nms.MinecraftVersion.INSTANCE.getMajor() >= 5) {
            return itemStack.getType().getKey().getKey();
        } else {
            net.minecraft.server.v1_12_R1.Item item = ((net.minecraft.server.v1_12_R1.ItemStack) nmsItem).getItem();
            String name = new Reflex(net.minecraft.server.v1_12_R1.Item.class).instance(item).get("name");
            String r = "";
            for (char c : name.toCharArray()) {
                if (Character.isUpperCase(c)) {
                    r += "_" + Character.toLowerCase(c);
                } else {
                    r += c;
                }
            }
            return r;
        }
    }

    @Override
    @NotNull
    public String getName(org.bukkit.inventory.ItemStack itemStack) {
        Object nmsItem = CraftItemStack.asNMSCopy(itemStack);
        if (taboolib.module.nms.MinecraftVersion.INSTANCE.getMajor() >= 5) {
            String name = ((net.minecraft.server.v1_8_R3.ItemStack) nmsItem).getItem().getName();
            if (itemStack.getItemMeta() instanceof PotionMeta) {
                name += ".effect." + ((net.minecraft.server.v1_8_R3.ItemStack) nmsItem).getTag().getString("Potion").replaceAll("minecraft:(strong_|long_)?", "");
            }
            return name;
        } else if (taboolib.module.nms.MinecraftVersion.INSTANCE.getMajor() >= 3) {
            String name = ((net.minecraft.server.v1_12_R1.ItemStack) nmsItem).getItem().a((net.minecraft.server.v1_12_R1.ItemStack) nmsItem);
            if (itemStack.getItemMeta() instanceof PotionMeta) {
                return name.replace("item.", "") + ".effect." + ((net.minecraft.server.v1_8_R3.ItemStack) nmsItem).getTag().getString("Potion").replaceAll("(minecraft:)?(strong_|long_)?", "");
            }
            return name + ".name";
        } else {
            String name = ((net.minecraft.server.v1_8_R3.ItemStack) nmsItem).getItem().getName();
            if (itemStack.getItemMeta() instanceof PotionMeta) {
                return name.replace("item.", "") + ".effect." + ((net.minecraft.server.v1_8_R3.ItemStack) nmsItem).getTag().getString("Potion").replaceAll("(minecraft:)?(strong_|long_)?", "");
            }
            return name + ".name";
        }
    }

    @Override
    @NotNull
    public String getName(Entity entity) {
        if (taboolib.module.nms.MinecraftVersion.INSTANCE.getMajor() >= 6) {
            Object minecraftKey = net.minecraft.server.v1_14_R1.EntityTypes.getName(((org.bukkit.craftbukkit.v1_14_R1.entity.CraftEntity) entity).getHandle().getEntityType());
            return "entity.minecraft." + ((net.minecraft.server.v1_14_R1.MinecraftKey) minecraftKey).getKey();
        } else if (taboolib.module.nms.MinecraftVersion.INSTANCE.getMajor() >= 5) {
            try {
                String name = "entity.minecraft." + IRegistry.ENTITY_TYPE.getKey(Ref.INSTANCE.get(((org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity) entity).getHandle(), entityTypesField)).getKey();
                if (entity instanceof Villager && ((CraftVillager) entity).getCareer() != null) {
                    name += "." + String.valueOf(((CraftVillager) entity).getCareer()).toLowerCase(Locale.getDefault());
                }
                return name;
            } catch (Throwable t) {
                t.printStackTrace();
            }
            return "entity.null";
        } else {
            try {
                if (entity instanceof Player) {
                    return "entity.Player.name";
                }
                if (entity instanceof Villager) {
                    String name = "name";
                    Object villager = ((org.bukkit.craftbukkit.v1_12_R1.entity.CraftVillager) entity).getHandle();
                    Object export = new NBTTagCompound();
                    ((EntityVillager) villager).b((NBTTagCompound) export);
                    int career = ((NBTTagCompound) export).getInt("Career");
                    switch (((EntityVillager) villager).getProfession()) {
                        case 0:
                            if (career == 1) {
                                name = "farmer";
                            } else if (career == 2) {
                                name = "fisherman";
                            } else if (career == 3) {
                                name = "shepherd";
                            } else if (career == 4) {
                                name = "fletcher";
                            }
                            break;
                        case 1:
                            if (career == 1) {
                                name = "librarian";
                            } else if (career == 2) {
                                name = "cartographer";
                            }
                            break;
                        case 2:
                            name = "cleric";
                            break;
                        case 3:
                            if (career == 1) {
                                name = "armor";
                            } else if (career == 2) {
                                name = "weapon";
                            } else if (career == 3) {
                                name = "tool";
                            }
                            break;
                        case 4:
                            if (career == 1) {
                                name = "butcher";
                            } else if (career == 2) {
                                name = "leather";
                            }
                            break;
                        case 5:
                            name = "nitwit";
                            break;
                    }
                    return "entity.Villager." + name;
                }
                return "entity." + entity.getType().getEntityClass().getSimpleName() + ".name";
            } catch (Throwable ignore) {
            }
            return "entity.null";
        }
    }

    @NotNull
    @Override
    public ItemTag getItemTag(org.bukkit.inventory.ItemStack itemStack) {
        Object nmsItem = CraftItemStack.asNMSCopy(itemStack);
        net.minecraft.server.v1_8_R3.ItemStack nmsItemStack = (net.minecraft.server.v1_8_R3.ItemStack) nmsItem;
        return (nmsItemStack).hasTag() ? fromNBTBase((nmsItemStack).getTag()).asCompound() : new ItemTag();
    }

    @Override
    public @NotNull
    ItemStack setItemTag(ItemStack itemStack, ItemTag compound) {
        Object nmsItem = CraftItemStack.asNMSCopy(itemStack);
        ((net.minecraft.server.v1_8_R3.ItemStack) nmsItem).setTag((net.minecraft.server.v1_8_R3.NBTTagCompound) toNBTBase(compound));
        return CraftItemStack.asBukkitCopy((net.minecraft.server.v1_8_R3.ItemStack) nmsItem);
    }

    @NotNull
    @Override
    public String itemTagToString(ItemTagData itemTag) {
        return toNBTBase(itemTag).toString();
    }

    private Object toNBTBase(ItemTagData base) {
        boolean v11500 = taboolib.module.nms.MinecraftVersion.INSTANCE.getMajor() >= 7;
        switch (base.getType().getId()) {
            case 1:
                if (v11500) {
                    return net.minecraft.server.v1_15_R1.NBTTagByte.a(base.asByte());
                } else {
                    return new NBTTagByte(base.asByte());
                }
            case 2:
                if (v11500) {
                    return net.minecraft.server.v1_15_R1.NBTTagShort.a(base.asShort());
                } else {
                    return new NBTTagShort(base.asShort());
                }
            case 3:
                if (v11500) {
                    return net.minecraft.server.v1_15_R1.NBTTagInt.a(base.asInt());
                } else {
                    return new NBTTagInt(base.asInt());
                }
            case 4:
                if (v11500) {
                    return net.minecraft.server.v1_15_R1.NBTTagLong.a(base.asLong());
                } else {
                    return new NBTTagLong(base.asLong());
                }
            case 5:
                if (v11500) {
                    return net.minecraft.server.v1_15_R1.NBTTagFloat.a(base.asFloat());
                } else {
                    return new NBTTagFloat(base.asFloat());
                }
            case 6:
                if (v11500) {
                    return net.minecraft.server.v1_15_R1.NBTTagDouble.a(base.asDouble());
                } else {
                    return new NBTTagDouble(base.asDouble());
                }
            case 7:
                return new NBTTagByteArray(base.asByteArray());
            case 11:
                return new NBTTagIntArray(base.asIntArray());
            case 8:
                if (v11500) {
                    return net.minecraft.server.v1_15_R1.NBTTagString.a(base.asString());
                } else {
                    return new NBTTagString(base.asString());
                }
            case 9:
                Object nmsList = new NBTTagList();
                for (ItemTagData value : base.asList()) {
                    // 1.14+
                    if (taboolib.module.nms.MinecraftVersion.INSTANCE.getMajor() >= 6) {
                        ((net.minecraft.server.v1_14_R1.NBTTagList) nmsList).add(((net.minecraft.server.v1_14_R1.NBTTagList) nmsList).size(), (net.minecraft.server.v1_14_R1.NBTBase) toNBTBase(value));
                    }
                    // 1.13
                    else if (taboolib.module.nms.MinecraftVersion.INSTANCE.getMajor() >= 5) {
                        ((net.minecraft.server.v1_13_R2.NBTTagList) nmsList).add((net.minecraft.server.v1_13_R2.NBTBase) toNBTBase(value));
                    }
                    // 1.12-
                    else {
                        ((NBTTagList) nmsList).add((net.minecraft.server.v1_8_R3.NBTBase) toNBTBase(value));
                    }
                }
                return nmsList;
            case 10:
                Object nmsTag = new net.minecraft.server.v1_8_R3.NBTTagCompound();
                for (Map.Entry<String, ItemTagData> entry : base.asCompound().entrySet()) {
                    ((Map) new Reflex(NBTTagCompound.class).instance(nmsTag).get("map")).put(entry.getKey(), toNBTBase(entry.getValue()));
                }
                return nmsTag;
        }
        return null;
    }

    private ItemTagData fromNBTBase(Object base) {
        if (base instanceof net.minecraft.server.v1_8_R3.NBTTagCompound) {
            ItemTag itemTag = new ItemTag();
            Map<String, net.minecraft.server.v1_12_R1.NBTBase> map = new Reflex(NBTTagCompound.class).instance(base).get("map");
            for (Map.Entry<String, net.minecraft.server.v1_12_R1.NBTBase> entry : map.entrySet()) {
                itemTag.put(entry.getKey(), (ItemTagData) fromNBTBase(entry.getValue()));
            }
            return itemTag;
        } else if (base instanceof NBTTagList) {
            ItemTagList itemTagList = new ItemTagList();
            List list = new Reflex(NBTTagList.class).instance(base).get("list");
            for (Object v : list) {
                itemTagList.add((ItemTagData) fromNBTBase(v));
            }
            return itemTagList;
        } else if (base instanceof NBTTagString) {
            return new ItemTagData(new Reflex(NBTTagString.class).instance(base).get("data", ""));
        } else if (base instanceof NBTTagDouble) {
            return new ItemTagData(new Reflex(NBTTagDouble.class).instance(base).get("data", 0D));
        } else if (base instanceof NBTTagInt) {
            return new ItemTagData(new Reflex(NBTTagInt.class).instance(base).get("data", 0));
        } else if (base instanceof NBTTagFloat) {
            return new ItemTagData(new Reflex(NBTTagFloat.class).instance(base).get("data", (float) 0));
        } else if (base instanceof NBTTagShort) {
            return new ItemTagData(new Reflex(NBTTagShort.class).instance(base).get("data", (short) 0));
        } else if (base instanceof NBTTagLong) {
            return new ItemTagData(new Reflex(NBTTagLong.class).instance(base).get("data", 0L));
        } else if (base instanceof NBTTagByte) {
            return new ItemTagData(new Reflex(NBTTagByte.class).instance(base).get("data", (byte) 0D));
        } else if (base instanceof NBTTagIntArray) {
            return new ItemTagData(new Reflex(NBTTagIntArray.class).instance(base).get("data", new int[0]));
        } else if (base instanceof NBTTagByteArray) {
            return new ItemTagData(new Reflex(NBTTagByteArray.class).instance(base).get("data", new byte[0]));
        }
        return null;
    }

    @Nullable
    public Object getEntityType(String name) {
        if (taboolib.module.nms.MinecraftVersion.INSTANCE.getMajor() >= 6) {
            return net.minecraft.server.v1_14_R1.EntityTypes.a(name).orElse(null);
        } else {
            return net.minecraft.server.v1_13_R2.EntityTypes.a(name);
        }
    }

    @Override
    public <T extends Entity> T spawnEntity(Location location, Class<T> entity, Consumer<T> e) {
        if (taboolib.module.nms.MinecraftVersion.INSTANCE.getMajor() >= 4) {
            return location.getWorld().spawn(location, entity, e::accept);
        } else {
            Object createEntity = ((CraftWorld) location.getWorld()).createEntity(location, entity);
            try {
                e.accept((T) ((net.minecraft.server.v1_13_R2.Entity) createEntity).getBukkitEntity());
            } catch (Throwable t) {
                t.printStackTrace();
            }
            return ((CraftWorld) location.getWorld()).addEntity((net.minecraft.server.v1_13_R2.Entity) createEntity, CreatureSpawnEvent.SpawnReason.CUSTOM);
        }
    }

    @Override
    public boolean createLight(Block block, LightType lightType, int lightLevel) {
        int level = getRawLightLevel(block, lightType);
        setRawLightLevel(block, lightType, lightLevel);
        recalculateLightAround(block, lightType, lightLevel);
        return getRawLightLevel(block, lightType) >= level;
    }

    @Override
    public boolean deleteLight(Block block, LightType lightType) {
        int level = getRawLightLevel(block, lightType);
        setRawLightLevel(block, lightType, 0);
        recalculateLightAround(block, lightType, level);
        return getRawLightLevel(block, lightType) != level;
    }

    @Override
    public int getRawLightLevel(Block block, LightType lightType) {
        Object world = ((CraftWorld) block.getWorld()).getHandle();
        Object position = new net.minecraft.server.v1_15_R1.BlockPosition(block.getX(), block.getY(), block.getZ());
        if (taboolib.module.nms.MinecraftVersion.INSTANCE.getMajor() >= 4) {
            if (lightType == LightType.BLOCK) {
                return ((net.minecraft.server.v1_13_R2.WorldServer) world).getBrightness(net.minecraft.server.v1_13_R2.EnumSkyBlock.BLOCK, (net.minecraft.server.v1_13_R2.BlockPosition) position);
            } else if (lightType == LightType.SKY) {
                return ((net.minecraft.server.v1_13_R2.WorldServer) world).getBrightness(net.minecraft.server.v1_13_R2.EnumSkyBlock.SKY, (net.minecraft.server.v1_13_R2.BlockPosition) position);
            } else {
                return ((net.minecraft.server.v1_13_R2.WorldServer) world).getLightLevel((net.minecraft.server.v1_13_R2.BlockPosition) position);
            }
        } else {
            Object chunk = ((net.minecraft.server.v1_9_R2.WorldServer) world).getChunkAt(block.getChunk().getX(), block.getChunk().getZ());
            if (lightType == LightType.BLOCK) {
                return ((net.minecraft.server.v1_9_R2.Chunk) chunk).getBrightness(net.minecraft.server.v1_9_R2.EnumSkyBlock.BLOCK, (net.minecraft.server.v1_9_R2.BlockPosition) position);
            } else if (lightType == LightType.SKY) {
                return ((net.minecraft.server.v1_9_R2.Chunk) world).getBrightness(net.minecraft.server.v1_9_R2.EnumSkyBlock.SKY, (net.minecraft.server.v1_9_R2.BlockPosition) position);
            } else {
                return 15;
            }
        }
    }

    @Override
    public void setRawLightLevel(Block block, LightType lightType, int lightLevel) {
        int level = Math.max(Math.min(lightLevel, 15), 0);
        Object world = ((CraftWorld) block.getWorld()).getHandle();
        Object position = new net.minecraft.server.v1_15_R1.BlockPosition(block.getX(), block.getY(), block.getZ());
        if (taboolib.module.nms.MinecraftVersion.INSTANCE.getMajor() >= 6) {
            syncLight(((net.minecraft.server.v1_14_R1.WorldServer) world).getChunkProvider().getLightEngine(), lightEngine -> {
                if (lightType == LightType.BLOCK) {
                    Object lightEngineLayer = ((net.minecraft.server.v1_14_R1.LightEngineThreaded) lightEngine).a(net.minecraft.server.v1_14_R1.EnumSkyBlock.BLOCK);
                    setRawLightLevelBlock(level, position, lightEngineLayer);
                } else if (lightType == LightType.SKY) {
                    Object lightEngineLayer = ((net.minecraft.server.v1_14_R1.LightEngineThreaded) lightEngine).a(net.minecraft.server.v1_14_R1.EnumSkyBlock.SKY);
                    setRawLightLevelSky(level, position, lightEngineLayer);
                } else {
                    Object lightEngineLayer1 = ((net.minecraft.server.v1_14_R1.LightEngineThreaded) lightEngine).a(net.minecraft.server.v1_14_R1.EnumSkyBlock.BLOCK);
                    Object lightEngineLayer2 = ((net.minecraft.server.v1_14_R1.LightEngineThreaded) lightEngine).a(net.minecraft.server.v1_14_R1.EnumSkyBlock.SKY);
                    setRawLightLevelBlock(level, position, lightEngineLayer1);
                    setRawLightLevelSky(level, position, lightEngineLayer2);
                }
            });
        } else {
            if (lightType == LightType.BLOCK) {
                ((net.minecraft.server.v1_13_R2.WorldServer) world).a(net.minecraft.server.v1_13_R2.EnumSkyBlock.BLOCK, (net.minecraft.server.v1_13_R2.BlockPosition) position, level);
            } else if (lightType == LightType.SKY) {
                ((net.minecraft.server.v1_13_R2.WorldServer) world).a(net.minecraft.server.v1_13_R2.EnumSkyBlock.SKY, (net.minecraft.server.v1_13_R2.BlockPosition) position, level);
            } else {
                ((net.minecraft.server.v1_13_R2.WorldServer) world).a(net.minecraft.server.v1_13_R2.EnumSkyBlock.BLOCK, (net.minecraft.server.v1_13_R2.BlockPosition) position, level);
                ((net.minecraft.server.v1_13_R2.WorldServer) world).a(net.minecraft.server.v1_13_R2.EnumSkyBlock.SKY, (net.minecraft.server.v1_13_R2.BlockPosition) position, level);
            }
        }
    }

    @Override
    public void recalculateLight(Block block, LightType lightType) {
        Object world = ((CraftWorld) block.getWorld()).getHandle();
        Object position = new net.minecraft.server.v1_15_R1.BlockPosition(block.getX(), block.getY(), block.getZ());
        if (taboolib.module.nms.MinecraftVersion.INSTANCE.getMajor() >= 6) {
            Object lightEngine = ((net.minecraft.server.v1_14_R1.WorldServer) world).getChunkProvider().getLightEngine();
            if (((net.minecraft.server.v1_14_R1.LightEngineThreaded) lightEngine).a()) {
                syncLight(lightEngine, e -> {
                    if (lightType == LightType.BLOCK) {
                        ((LightEngineLayer) ((net.minecraft.server.v1_14_R1.LightEngineThreaded) lightEngine).a(net.minecraft.server.v1_14_R1.EnumSkyBlock.BLOCK)).a(Integer.MAX_VALUE, true, true);
                    } else if (lightType == LightType.SKY) {
                        ((LightEngineLayer) ((net.minecraft.server.v1_14_R1.LightEngineThreaded) lightEngine).a(net.minecraft.server.v1_14_R1.EnumSkyBlock.SKY)).a(Integer.MAX_VALUE, true, true);
                    } else {
                        Object b = ((net.minecraft.server.v1_14_R1.LightEngineThreaded) lightEngine).a(net.minecraft.server.v1_14_R1.EnumSkyBlock.BLOCK);
                        Object s = ((net.minecraft.server.v1_14_R1.LightEngineThreaded) lightEngine).a(net.minecraft.server.v1_14_R1.EnumSkyBlock.SKY);
                        int maxUpdateCount = Integer.MAX_VALUE;
                        int integer4 = maxUpdateCount / 2;
                        int integer5 = ((LightEngineLayer) b).a(integer4, true, true);
                        int integer6 = maxUpdateCount - integer4 + integer5;
                        int integer7 = ((LightEngineLayer) s).a(integer6, true, true);
                        if (integer5 == 0 && integer7 > 0) {
                            ((LightEngineLayer) b).a(integer7, true, true);
                        }
                    }
                });
            }
        } else {
            if (lightType == LightType.SKY) {
                ((net.minecraft.server.v1_13_R2.WorldServer) world).c(net.minecraft.server.v1_13_R2.EnumSkyBlock.SKY, (net.minecraft.server.v1_13_R2.BlockPosition) position);
            } else if (lightType == LightType.BLOCK) {
                ((net.minecraft.server.v1_13_R2.WorldServer) world).c(net.minecraft.server.v1_13_R2.EnumSkyBlock.BLOCK, (net.minecraft.server.v1_13_R2.BlockPosition) position);
            } else {
                ((net.minecraft.server.v1_13_R2.WorldServer) world).c(net.minecraft.server.v1_13_R2.EnumSkyBlock.SKY, (net.minecraft.server.v1_13_R2.BlockPosition) position);
                ((net.minecraft.server.v1_13_R2.WorldServer) world).c(net.minecraft.server.v1_13_R2.EnumSkyBlock.BLOCK, (net.minecraft.server.v1_13_R2.BlockPosition) position);
            }
        }
    }

    @Override
    public void recalculateLightAround(Block block, LightType lightType, int lightLevel) {
        // ?????????????????????????????? ??????????????????
        if (getRawLightLevel(block.getRelative(1, 0, 0), lightType) < lightLevel) {
            recalculateLight(block.getRelative(1, 0, 0), lightType);
        }
        if (getRawLightLevel(block.getRelative(-1, 0, 0), lightType) < lightLevel) {
            recalculateLight(block.getRelative(-1, 0, 0), lightType);
        }
        if (getRawLightLevel(block.getRelative(0, 1, 0), lightType) < lightLevel) {
            recalculateLight(block.getRelative(0, 1, 0), lightType);
        }
        if (getRawLightLevel(block.getRelative(0, -1, 0), lightType) < lightLevel) {
            recalculateLight(block.getRelative(0, -1, 0), lightType);
        }
        if (getRawLightLevel(block.getRelative(0, 0, 1), lightType) < lightLevel) {
            recalculateLight(block.getRelative(0, 0, 1), lightType);
        }
        if (getRawLightLevel(block.getRelative(0, 0, -1), lightType) < lightLevel) {
            recalculateLight(block.getRelative(0, 0, -1), lightType);
        }
    }

    @Override
    public void updateLight(Chunk chunk) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Object human = ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) player).getHandle();
            Object chunk1 = ((CraftWorld) player.getWorld()).getHandle().getChunkAt(chunk.getX(), chunk.getZ());
            Object chunk2 = ((net.minecraft.server.v1_8_R3.EntityPlayer) human).world.getChunkAtWorldCoords(((net.minecraft.server.v1_8_R3.EntityPlayer) human).getChunkCoordinates());
            if (distance(chunk2, chunk1) < distance(human)) {
                if (taboolib.module.nms.MinecraftVersion.INSTANCE.getMajor() >= 8) {
                    sendPacket(player, new net.minecraft.server.v1_16_R1.PacketPlayOutLightUpdate(((net.minecraft.server.v1_16_R1.Chunk) chunk1).getPos(), ((net.minecraft.server.v1_16_R1.Chunk) chunk1).e(), true));
                } else if (taboolib.module.nms.MinecraftVersion.INSTANCE.getMajor() >= 6) {
                    sendPacket(player, new PacketPlayOutLightUpdate(((net.minecraft.server.v1_14_R1.Chunk) chunk1).getPos(), ((net.minecraft.server.v1_14_R1.Chunk) chunk1).e()));
                } else {
                    sendPacket(player, new net.minecraft.server.v1_14_R1.PacketPlayOutMapChunk((net.minecraft.server.v1_14_R1.Chunk) chunk1, 0x1ffff));
                }
            }
        }
    }

    @Override
    @NotNull
    public String getEnchantmentKey(Enchantment enchantment) {
        if (taboolib.module.nms.MinecraftVersion.INSTANCE.getMajor() >= 5) {
            return "enchantment.minecraft." + new Reflex(Keyed.class).instance(enchantment).<NamespacedKey>invoke("getKey").getKey();
        } else {
            return net.minecraft.server.v1_13_R2.IRegistry.ENCHANTMENT.fromId(new Reflex(Enchantment.class).instance(enchantment).get("id")).g();
        }
    }

    @Override
    @NotNull
    public String getPotionEffectTypeKey(PotionEffectType potionEffectType) {
        if (taboolib.module.nms.MinecraftVersion.INSTANCE.getMajor() >= 5) {
            return net.minecraft.server.v1_13_R2.MobEffectList.fromId(potionEffectType.getId()).c();
        } else {
            return net.minecraft.server.v1_12_R1.MobEffectList.fromId(potionEffectType.getId()).a();
        }
    }

    private int distance(Object player) {
        int viewDistance = Bukkit.getViewDistance();
        try {
            int playerViewDistance = ((net.minecraft.server.v1_14_R1.EntityPlayer) player).clientViewDistance;
            if (playerViewDistance < viewDistance) {
                viewDistance = playerViewDistance;
            }
        } catch (Throwable ignored) {
        }
        return viewDistance;
    }

    private int distance(Object from, Object to) {
        if (taboolib.module.nms.MinecraftVersion.INSTANCE.getMajor() >= 8) {
            String name1 = ((WorldDataServer) ((net.minecraft.server.v1_16_R1.Chunk) from).world.getWorldData()).getName();
            String name2 = ((WorldDataServer) ((net.minecraft.server.v1_16_R1.Chunk) to).world.getWorldData()).getName();
            if (!name1.equals(name2)) {
                return 100;
            }
        } else {
            if (!((net.minecraft.server.v1_14_R1.Chunk) from).world.getWorldData().getName().equals(((net.minecraft.server.v1_14_R1.Chunk) to).world.getWorldData().getName())) {
                return 100;
            }
        }
        double var2;
        double var4;
        if (taboolib.module.nms.MinecraftVersion.INSTANCE.getMajor() >= 4) {
            var2 = ((net.minecraft.server.v1_12_R1.Chunk) to).locX - ((net.minecraft.server.v1_12_R1.Chunk) from).locX;
            var4 = ((net.minecraft.server.v1_12_R1.Chunk) to).locZ - ((net.minecraft.server.v1_12_R1.Chunk) from).locZ;
        } else {
            var2 = ((net.minecraft.server.v1_14_R1.Chunk) to).getPos().x - ((net.minecraft.server.v1_14_R1.Chunk) from).getPos().x;
            var4 = ((net.minecraft.server.v1_14_R1.Chunk) to).getPos().z - ((net.minecraft.server.v1_14_R1.Chunk) from).getPos().z;
        }
        return (int) Math.sqrt(var2 * var2 + var4 * var4);
    }

    private void syncLight(Object lightEngine, Consumer<Object> task) {
        try {
            Object b = new Reflex(LightEngineThreaded.class).instance(lightEngine).get("b");
            Object c = new Reflex(ThreadedMailbox.class).instance(b).get("c");

            int flags;
            long wait = -1L;

            while (!((AtomicInteger) c).compareAndSet(flags = ((AtomicInteger) c).get() & ~2, flags | 2)) {
                if ((flags & 1) != 0) {
                    if (wait == -1) {
                        wait = System.currentTimeMillis() + 3 * 1000;
                        FunctionKt.info("ThreadedMailbox is closing. Will wait...");
                    } else if (System.currentTimeMillis() >= wait) {
                        FunctionKt.warning("Failed to enter critical section while ThreadedMailbox is closing");
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ignored) {
                    }
                }
            }

            try {
                task.accept(lightEngine);
            } finally {
                while (!((AtomicInteger) c).compareAndSet(flags = ((AtomicInteger) c).get(), flags & ~2)) {
                }
                new Reflex(ThreadedMailbox.class).instance(b).invoke("f");
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void setRawLightLevelBlock(int level, Object position, Object lightEngineLayer) {
        if (level == 0) {
            ((LightEngineBlock) lightEngineLayer).a((BlockPosition) position);
        } else if (((LightEngineLayer) lightEngineLayer).a(SectionPosition.a((net.minecraft.server.v1_14_R1.BlockPosition) position)) != null) {
            try {
                ((LightEngineLayer) lightEngineLayer).a((net.minecraft.server.v1_14_R1.BlockPosition) position, level);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private void setRawLightLevelSky(int level, Object position, Object lightEngineLayer) {
        if (level == 0) {
            ((LightEngineSky) lightEngineLayer).a((BlockPosition) position);
        } else if (((LightEngineLayer) lightEngineLayer).a(SectionPosition.a((net.minecraft.server.v1_14_R1.BlockPosition) position)) != null) {
            try {
                Object s = new Reflex(LightEngineLayer.class).instance(lightEngineLayer).get("c");
                if (MinecraftVersion.INSTANCE.getMajor() >= 7) {
                    new Reflex(LightEngineStorage.class).instance(s).invoke("d");
                } else {
                    new Reflex(LightEngineStorage.class).instance(s).invoke("c");
                }
                new Reflex(LightEngineGraph.class).instance(lightEngineLayer).invoke("a", 9223372036854775807L, ((net.minecraft.server.v1_14_R1.BlockPosition) position).asLong(), 15 - level, true);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
