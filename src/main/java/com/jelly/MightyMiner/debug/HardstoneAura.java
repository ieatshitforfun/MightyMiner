package com.jelly.MightyMiner.debug;

import com.jelly.MightyMiner.utils.PlayerUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.BlockStainedGlassPane;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.util.*;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;
import rosegoldaddons.Main;
import rosegoldaddons.events.PlayerMoveEvent;
import rosegoldaddons.events.ReceivePacketEvent;
import rosegoldaddons.utils.PlayerUtils;
import rosegoldaddons.utils.RenderUtils;
import rosegoldaddons.utils.ShadyRotation;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HardstoneAura {
    public static boolean autoHardStone;
    public static int hardIndex;
    public static boolean includeOres;
    private ArrayList<Vec3> solved = new ArrayList();
    private ArrayList<BlockPos> broken = new ArrayList();
    private static int currentDamage;
    private static BlockPos closestStone;
    private static Vec3 closestChest;
    private static Vec3 particlePos;
    private boolean stopHardstone = false;
    private static int ticks = 0;
    private static BlockPos gemstone;
    private static BlockPos lastGem;
    
    static Minecraft mc = Minecraft.getMinecraft();
    private boolean ignoreTitanium;
    private boolean includeExcavatable;
    private boolean guilag;
    private int lineWidth;
    private int smoothLookVelocity;
    private int hardrange;
    private boolean serverSideChest;


    public HardstoneAura() {
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!autoHardStone) {
            currentDamage = 0;
            this.broken.clear();
        } else {
            if (!this.stopHardstone) {
                particlePos = null;
                ++ticks;
                if (hardIndex == 0 && this.broken.size() > 10) {
                    this.broken.clear();
                }

                if (hardIndex == 1 && this.broken.size() > 6) {
                    this.broken.clear();
                }

                if (ticks > 30) {
                    this.broken.clear();
                    ticks = 0;
                }

                closestStone = this.closestStone();
                if (currentDamage > 200) {
                    currentDamage = 0;
                }

                if (gemstone != null && mc.thePlayer != null) {
                    if (lastGem != null && !lastGem.equals(gemstone)) {
                        currentDamage = 0;
                    }

                    lastGem = gemstone;
                    if (currentDamage == 0) {
                        mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, gemstone, EnumFacing.DOWN));
                    }

                    PlayerUtils.swingItem();
                    ++currentDamage;
                }

                if (closestStone != null && gemstone == null) {
                    currentDamage = 0;
                    MovingObjectPosition fake = mc.objectMouseOver;
                    fake.hitVec = new Vec3(closestStone);
                    EnumFacing enumFacing = fake.sideHit;
                    if (enumFacing != null && mc.thePlayer != null) {
                        mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, closestStone, enumFacing));
                    }

                    PlayerUtils.swingItem();
                    this.broken.add(closestStone);
                }
            }

        }
    }

    @SubscribeEvent
    public void receivePacket(ReceivePacketEvent event) {
        if (autoHardStone) {
            if (event.packet instanceof S2APacketParticles) {
                S2APacketParticles packet = (S2APacketParticles)event.packet;
                if (packet.getParticleType().equals(EnumParticleTypes.CRIT)) {
                    Vec3 particlePos = new Vec3(packet.getXCoordinate(), packet.getYCoordinate(), packet.getZCoordinate());
                    if (closestChest != null) {
                        this.stopHardstone = true;
                        double dist = closestChest.distanceTo(particlePos);
                        if (dist < 1.0) {
                            if (!Main.configFile.serverSideChest) {
                                ShadyRotation.smoothLook(ShadyRotation.vec3ToRotation(particlePos), smoothLookVelocity, () -> {
                                });
                            } else {
                                particlePos = particlePos;
                            }
                        }
                    }
                }
            }

        }
    }

    @SubscribeEvent(
            priority = EventPriority.NORMAL
    )
    public void onUpdatePre(PlayerMoveEvent.Pre pre) {
        if (particlePos != null && serverSideChest) {
            ShadyRotation.smoothLook(ShadyRotation.vec3ToRotation(particlePos), 0, () -> {
            });
        }

    }

    @SubscribeEvent(
            priority = EventPriority.HIGHEST
    )
    public void chat(ClientChatReceivedEvent event) {
        if (event.type == 0) {
            String message = event.message.getUnformattedText();
            if (message.contains("You have successfully picked the lock on this chest!") && particlePos != null && this.stopHardstone) {
                this.solved.add(closestChest);
                particlePos = null;
                this.stopHardstone = false;
            }

        }
    }

    @SubscribeEvent
    public void guiDraw(GuiScreenEvent.BackgroundDrawnEvent event) {
        if (guilag) {
            mc.gameSettings.setOptionFloatValue(GameSettings.Options.FRAMERATE_LIMIT, 1.0F);
        }

        if (autoHardStone) {
            if (event.gui instanceof GuiChest) {
                Container container = ((GuiChest)event.gui).inventorySlots;
                if (container instanceof ContainerChest) {
                    String chestName = ((ContainerChest)container).getLowerChestInventory().getDisplayName().getUnformattedText();
                    if (chestName.contains("Treasure")) {
                        this.solved.add(closestChest);
                        particlePos = null;
                        this.stopHardstone = false;
                        mc.thePlayer.closeScreen();
                    }
                }
            }

        }
    }


    private static final Map<Integer, Boolean> glCapMap = new HashMap<Integer, Boolean>();

    public static void setGlCap(int cap, boolean state) {
        glCapMap.put(cap, GL11.glGetBoolean((int)cap));
        setGlState(cap, state);
    }

    public static void disableGlCap(int cap) {
        setGlCap(cap, true);
    }

    public static void disableGlCap(int ... caps) {
        for (int cap : caps) {
            setGlCap(cap, false);
        }
    }

    public static void setGlState(int cap, boolean state) {
        if (state) {
            GL11.glEnable((int)cap);
        } else {
            GL11.glDisable((int)cap);
        }
    }

    public static void enableGlCap(int cap) {
        setGlCap(cap, true);
    }

    public static void glColor(int red, int green, int blue, int alpha) {
        GL11.glColor4f((float)((float)red / 255.0f), (float)((float)green / 255.0f), (float)((float)blue / 255.0f), (float)((float)alpha / 255.0f));
    }

    public static void glColor(Color color) {
        glColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    private static void glColor(int hex) {
        glColor(hex >> 16 & 0xFF, hex >> 8 & 0xFF, hex & 0xFF, hex >> 24 & 0xFF);
    }

    public static void drawSelectionBoundingBox(AxisAlignedBB boundingBox) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(3, DefaultVertexFormats.POSITION);
        worldrenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        worldrenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        worldrenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        worldrenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        worldrenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        worldrenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        worldrenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        worldrenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        worldrenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        worldrenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        worldrenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        worldrenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        worldrenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        worldrenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        worldrenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        worldrenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        tessellator.draw();
    }
    public static void resetCaps() {
        glCapMap.forEach(HardstoneAura::setGlState);
    }

    public static void drawBlockBox(BlockPos blockPos, Color color, int width, float partialTicks) {
        if (width == 0) {
            return;
        }
        RenderManager renderManager = mc.getRenderManager();
        double x = (double)blockPos.getX() - renderManager.viewerPosX;
        double y = (double)blockPos.getY() - renderManager.viewerPosY;
        double z = (double)blockPos.getZ() - renderManager.viewerPosZ;
        AxisAlignedBB axisAlignedBB = new AxisAlignedBB(x, y, z, x + 1.0, y + 1.0, z + 1.0);
        Block block = mc.theWorld.getBlockState(blockPos).getBlock();
        if (block != null) {
            EntityPlayerSP player = mc.thePlayer;
            double posX = ((EntityPlayer)player).lastTickPosX + (player.posX - ((EntityPlayer)player).lastTickPosX) * (double)partialTicks;
            double posY = ((EntityPlayer)player).lastTickPosY + (player.posY - ((EntityPlayer)player).lastTickPosY) * (double)partialTicks;
            double posZ = ((EntityPlayer)player).lastTickPosZ + (player.posZ - ((EntityPlayer)player).lastTickPosZ) * (double)partialTicks;
            block.setBlockBoundsBasedOnState((IBlockAccess) mc.theWorld, blockPos);
            axisAlignedBB = block.getSelectedBoundingBox((World)((Object)mc.theWorld), blockPos).expand(0.002f, 0.002f, 0.002f).offset(-posX, -posY, -posZ);
        }
        GL11.glBlendFunc((int)770, (int)771);
        enableGlCap(3042);
        disableGlCap(3553, 2929);
        GL11.glDepthMask((boolean)false);
        glColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() != 255 ? color.getAlpha() : 26);
        GL11.glLineWidth((float)width);
        enableGlCap(2848);
        glColor(color);
        drawSelectionBoundingBox(axisAlignedBB);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GL11.glDepthMask((boolean)true);
        resetCaps();
    }

    @SubscribeEvent
    public void renderWorld(RenderWorldLastEvent event) {
        if (autoHardStone) {
            closestStone = this.closestStone();
            closestChest = this.closestChest();
            if (closestStone != null) {
                drawBlockBox(closestStone, new Color(128, 128, 128), lineWidth, event.partialTicks);
            }

            if (closestChest != null) {
                drawBlockBox(new BlockPos(closestChest.xCoord, closestChest.yCoord, closestChest.zCoord), new Color(255, 128, 0), Main.configFile.lineWidth, event.partialTicks);
            } else {
                this.stopHardstone = false;
            }

            if (gemstone != null) {
                IBlockState blockState = mc.theWorld.getBlockState(gemstone);
                EnumDyeColor dyeColor = null;
                Color color = Color.BLACK;
                if (blockState.getBlock() == Blocks.stained_glass) {
                    dyeColor = (EnumDyeColor)blockState.getValue(BlockStainedGlass.COLOR);
                }

                if (blockState.getBlock() == Blocks.stained_glass_pane) {
                    dyeColor = (EnumDyeColor)blockState.getValue(BlockStainedGlassPane.COLOR);
                }

                if (dyeColor == EnumDyeColor.RED) {
                    color = new Color(188, 3, 29);
                } else if (dyeColor == EnumDyeColor.PURPLE) {
                    color = new Color(137, 0, 201);
                } else if (dyeColor == EnumDyeColor.LIME) {
                    color = new Color(157, 249, 32);
                } else if (dyeColor == EnumDyeColor.LIGHT_BLUE) {
                    color = new Color(60, 121, 224);
                } else if (dyeColor == EnumDyeColor.ORANGE) {
                    color = new Color(237, 139, 35);
                } else if (dyeColor == EnumDyeColor.YELLOW) {
                    color = new Color(249, 215, 36);
                } else if (dyeColor == EnumDyeColor.MAGENTA) {
                    color = new Color(214, 15, 150);
                }

                drawBlockBox(gemstone, color, lineWidth, event.partialTicks);
            }

        }
    }

    @SubscribeEvent
    public void clear(WorldEvent.Load event) {
        this.solved.clear();
    }

    private BlockPos closestStone() {
        if (mc.theWorld == null) {
            return null;
        } else if (mc.thePlayer == null) {
            return null;
        } else {
            int r = 4;
            BlockPos playerPos = mc.thePlayer.getPosition();
            playerPos.add(0, 1, 0);
            Vec3 playerVec = mc.thePlayer.getPositionVector();
            Vec3i vec3i = new Vec3i(r, 1 + hardrange, r);
            Vec3i vec3i2 = new Vec3i(r, Main.configFile.hardrangeDown, r);
            ArrayList<Vec3> stones = new ArrayList();
            ArrayList<Vec3> gemstones = new ArrayList();
            if (playerPos != null) {
                Iterator var8 = BlockPos.getAllInBox(playerPos.add(vec3i), playerPos.subtract(vec3i2)).iterator();

                label338:
                while(true) {
                    BlockPos blockPos;
                    IBlockState blockState;
                    do {
                        do {
                            int x;
                            int z;
                            do {
                                label307:
                                do {
                                    while(true) {
                                        do {
                                            if (!var8.hasNext()) {
                                                break label338;
                                            }

                                            blockPos = (BlockPos)var8.next();
                                            blockState = mc.theWorld.getBlockState(blockPos);
                                            if (hardIndex == 0) {
                                                if (!Main.configFile.includeExcavatable && blockState.getBlock() == Blocks.stone && !this.broken.contains(blockPos)) {
                                                    stones.add(new Vec3((double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5));
                                                }

                                                if (Main.configFile.includeOres && (blockState.getBlock() == Blocks.coal_ore || blockState.getBlock() == Blocks.diamond_ore || blockState.getBlock() == Blocks.gold_ore || blockState.getBlock() == Blocks.redstone_ore || blockState.getBlock() == Blocks.iron_ore || blockState.getBlock() == Blocks.lapis_ore || blockState.getBlock() == Blocks.emerald_ore || blockState.getBlock() == Blocks.netherrack || blockState.getBlock() == Blocks.lit_redstone_ore) && !this.broken.contains(blockPos)) {
                                                    stones.add(new Vec3((double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5));
                                                }

                                                if (Main.configFile.includeExcavatable && (blockState.getBlock() == Blocks.gravel || blockState.getBlock() == Blocks.sand) && !this.broken.contains(blockPos)) {
                                                    stones.add(new Vec3((double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5));
                                                }
                                            }
                                        } while(hardIndex != 1);

                                        EnumFacing dir = mc.thePlayer.getHorizontalFacing();
                                        x = (int)Math.floor(mc.thePlayer.posX);
                                        z = (int)Math.floor(mc.thePlayer.posZ);
                                        switch (dir) {
                                            case NORTH:
                                                if (blockPos.getZ() > z || blockPos.getX() != x) {
                                                    break;
                                                }

                                                if (this.isSlow(blockState)) {
                                                    gemstones.add(new Vec3((double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5));
                                                } else if (!Main.configFile.includeExcavatable && blockState.getBlock() == Blocks.stone && !this.broken.contains(blockPos)) {
                                                    stones.add(new Vec3((double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5));
                                                }

                                                if (Main.configFile.includeOres && (blockState.getBlock() == Blocks.coal_ore || blockState.getBlock() == Blocks.diamond_ore || blockState.getBlock() == Blocks.gold_ore || blockState.getBlock() == Blocks.redstone_ore || blockState.getBlock() == Blocks.iron_ore || blockState.getBlock() == Blocks.lapis_ore || blockState.getBlock() == Blocks.emerald_ore || blockState.getBlock() == Blocks.netherrack || blockState.getBlock() == Blocks.lit_redstone_ore) && !this.broken.contains(blockPos)) {
                                                    stones.add(new Vec3((double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5));
                                                }

                                                if (Main.configFile.includeExcavatable && (blockState.getBlock() == Blocks.gravel || blockState.getBlock() == Blocks.sand) && !this.broken.contains(blockPos)) {
                                                    stones.add(new Vec3((double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5));
                                                }
                                                break;
                                            case SOUTH:
                                                if (blockPos.getZ() < z || blockPos.getX() != x) {
                                                    break;
                                                }

                                                if (this.isSlow(blockState)) {
                                                    gemstones.add(new Vec3((double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5));
                                                } else if (!Main.configFile.includeExcavatable && blockState.getBlock() == Blocks.stone && !this.broken.contains(blockPos)) {
                                                    stones.add(new Vec3((double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5));
                                                }

                                                if (Main.configFile.includeOres && (blockState.getBlock() == Blocks.coal_ore || blockState.getBlock() == Blocks.diamond_ore || blockState.getBlock() == Blocks.gold_ore || blockState.getBlock() == Blocks.redstone_ore || blockState.getBlock() == Blocks.iron_ore || blockState.getBlock() == Blocks.lapis_ore || blockState.getBlock() == Blocks.emerald_ore || blockState.getBlock() == Blocks.netherrack || blockState.getBlock() == Blocks.lit_redstone_ore) && !this.broken.contains(blockPos)) {
                                                    stones.add(new Vec3((double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5));
                                                }

                                                if (Main.configFile.includeExcavatable && (blockState.getBlock() == Blocks.gravel || blockState.getBlock() == Blocks.sand) && !this.broken.contains(blockPos)) {
                                                    stones.add(new Vec3((double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5));
                                                }
                                                break;
                                            case WEST:
                                                if (blockPos.getX() > x || blockPos.getZ() != z) {
                                                    break;
                                                }

                                                if (this.isSlow(blockState)) {
                                                    gemstones.add(new Vec3((double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5));
                                                } else if (!Main.configFile.includeExcavatable && blockState.getBlock() == Blocks.stone && !this.broken.contains(blockPos)) {
                                                    stones.add(new Vec3((double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5));
                                                }

                                                if (Main.configFile.includeOres && (blockState.getBlock() == Blocks.coal_ore || blockState.getBlock() == Blocks.diamond_ore || blockState.getBlock() == Blocks.gold_ore || blockState.getBlock() == Blocks.redstone_ore || blockState.getBlock() == Blocks.iron_ore || blockState.getBlock() == Blocks.lapis_ore || blockState.getBlock() == Blocks.emerald_ore || blockState.getBlock() == Blocks.netherrack || blockState.getBlock() == Blocks.lit_redstone_ore) && !this.broken.contains(blockPos)) {
                                                    stones.add(new Vec3((double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5));
                                                }

                                                if (Main.configFile.includeExcavatable && (blockState.getBlock() == Blocks.gravel || blockState.getBlock() == Blocks.sand) && !this.broken.contains(blockPos)) {
                                                    stones.add(new Vec3((double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5));
                                                }
                                                break;
                                            case EAST:
                                                continue label307;
                                        }
                                    }
                                } while(blockPos.getX() < x);
                            } while(blockPos.getZ() != z);

                            if (this.isSlow(blockState)) {
                                gemstones.add(new Vec3((double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5));
                            } else if (!Main.configFile.includeExcavatable && blockState.getBlock() == Blocks.stone && !this.broken.contains(blockPos)) {
                                stones.add(new Vec3((double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5));
                            }

                            if (includeOres && (blockState.getBlock() == Blocks.coal_ore || blockState.getBlock() == Blocks.diamond_ore || blockState.getBlock() == Blocks.gold_ore || blockState.getBlock() == Blocks.redstone_ore || blockState.getBlock() == Blocks.iron_ore || blockState.getBlock() == Blocks.lapis_ore || blockState.getBlock() == Blocks.emerald_ore || blockState.getBlock() == Blocks.netherrack || blockState.getBlock() == Blocks.lit_redstone_ore) && !this.broken.contains(blockPos)) {
                                stones.add(new Vec3((double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5));
                            }
                        } while(!includeExcavatable);
                    } while(blockState.getBlock() != Blocks.gravel && blockState.getBlock() != Blocks.sand);

                    if (!this.broken.contains(blockPos)) {
                        stones.add(new Vec3((double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5));
                    }
                }
            }

            double smallest = 9999.0;
            Vec3 closest = null;
            Iterator var20 = stones.iterator();

            while(var20.hasNext()) {
                Vec3 stone = (Vec3)var20.next();
                double dist = stone.distanceTo(playerVec);
                if (dist < smallest) {
                    smallest = dist;
                    closest = stone;
                }
            }

            double smallestgem = 9999.0;
            Vec3 closestgem = null;
            Iterator var14 = gemstones.iterator();

            while(var14.hasNext()) {
                Vec3 gem = (Vec3)var14.next();
                double dist = gem.distanceTo(playerVec);
                if (dist < smallestgem) {
                    smallestgem = dist;
                    closestgem = gem;
                }
            }

            if (closestgem != null) {
                gemstone = new BlockPos(closestgem.xCoord, closestgem.yCoord, closestgem.zCoord);
            } else {
                gemstone = null;
            }

            if (closest != null && smallest < 2.0) {
                return new BlockPos(closest.xCoord, closest.yCoord, closest.zCoord);
            } else {
                return null;
            }
        }
    }

    private Vec3 closestChest() {
        if (mc.theWorld == null) {
            return null;
        } else if (mc.thePlayer == null) {
            return null;
        } else {
            int r = 6;
            BlockPos playerPos = mc.thePlayer.getPosition();
            playerPos.add(0, 1, 0);
            Vec3 playerVec = mc.thePlayer.getPositionVector();
            Vec3i vec3i = new Vec3i(r, r, r);
            ArrayList<Vec3> chests = new ArrayList();
            if (playerPos != null) {
                Iterator var6 = BlockPos.getAllInBox(playerPos.add(vec3i), playerPos.subtract(vec3i)).iterator();

                while(var6.hasNext()) {
                    BlockPos blockPos = (BlockPos)var6.next();
                    IBlockState blockState = mc.theWorld.getBlockState(blockPos);
                    if (blockState.getBlock() == Blocks.chest) {
                        chests.add(new Vec3((double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5));
                    }
                }
            }

            double smallest = 9999.0;
            Vec3 closest = null;
            Iterator var9 = chests.iterator();

            while(var9.hasNext()) {
                Vec3 chest = (Vec3)var9.next();
                if (!this.solved.contains(chest)) {
                    double dist = chest.distanceTo(playerVec);
                    if (dist < smallest) {
                        smallest = dist;
                        closest = chest;
                    }
                }
            }

            return closest;
        }
    }

    private boolean isSlow(IBlockState blockState) {
        if (blockState.getBlock() == Blocks.prismarine) {
            return true;
        } else if (blockState.getBlock() == Blocks.wool) {
            return true;
        } else if (blockState.getBlock() == Blocks.stained_hardened_clay) {
            return true;
        } else if (!ignoreTitanium && blockState.getBlock() == Blocks.stone && blockState.getValue(BlockStone.VARIANT) == BlockStone.EnumType.DIORITE_SMOOTH) {
            return true;
        } else if (blockState.getBlock() == Blocks.gold_block) {
            return true;
        } else {
            return blockState.getBlock() == Blocks.stained_glass_pane || blockState.getBlock() == Blocks.stained_glass;
        }
    }
}
