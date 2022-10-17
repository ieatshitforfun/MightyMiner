package com.jelly.MightyMiner.macros.macros;

import com.jelly.MightyMiner.MightyMiner;
import com.jelly.MightyMiner.baritone.automine.AutoMineBaritone;
import com.jelly.MightyMiner.baritone.automine.config.AutoMineType;
import com.jelly.MightyMiner.baritone.automine.config.MineBehaviour;
import com.jelly.MightyMiner.debug.HardstoneAura;
import com.jelly.MightyMiner.handlers.KeybindHandler;
import com.jelly.MightyMiner.handlers.MacroHandler;
import com.jelly.MightyMiner.macros.Macro;
import com.jelly.MightyMiner.player.Rotation;
import com.jelly.MightyMiner.utils.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;


public class PowderMacro extends Macro {

    List<Block> blocksAllowedToMine = new ArrayList<>();
    List<Block> mineSlowBlocks = new ArrayList<Block>(){
        {
            add(Blocks.prismarine);
            add(Blocks.stained_glass_pane);
            add(Blocks.stained_glass);
            add(Blocks.wool);
        }
    };
    long treasureInitialTime;

    float playerYaw;
    BlockPos uTurnCachePos;

    Rotation rotation = new Rotation();

    State currentState;
    State treasureCacheState;
    TreasureState treasureState = TreasureState.NONE;
    int centerToBlockTick = 0;

    boolean centering = false;
    int turnState = 0;

    AutoMineBaritone mineBaritone = new AutoMineBaritone(getAutomineConfig());
    boolean chestInStraightLine;
    BlockPos chest;
    BlockPos returnBlockPos;

    enum State {
        NORMAL,
        TREASURE,
        UTurn
    }

    enum TreasureState {
        NONE,
        WALKING,
        SOLVING,
        RETURNING

    }


    @Override
    public void onEnable() {

        if(PlayerUtils.isNearPlayer()){
            LogUtils.addMessage("Not stating, there is a player near");
            this.toggle();
            return;
        }

        HardstoneAura.hardIndex = MightyMiner.config.powAuraType;
        HardstoneAura.includeOres = true;
        currentState = State.NORMAL;
        turnState = 1;
        treasureInitialTime = System.currentTimeMillis();
        playerYaw = AngleUtils.getClosest();

        blocksAllowedToMine.clear();
        blocksAllowedToMine.add(Blocks.stone);
        blocksAllowedToMine.add(Blocks.air);
        blocksAllowedToMine.add(Blocks.coal_ore);
        blocksAllowedToMine.add(Blocks.iron_ore);
        blocksAllowedToMine.add(Blocks.emerald_ore);
        blocksAllowedToMine.add(Blocks.gold_ore);
        blocksAllowedToMine.add(Blocks.redstone_ore);
        blocksAllowedToMine.add(Blocks.lapis_ore);
        blocksAllowedToMine.add(Blocks.lit_redstone_ore);
        blocksAllowedToMine.add(Blocks.diamond_ore);
        blocksAllowedToMine.add(Blocks.prismarine);
        blocksAllowedToMine.add(Blocks.wool);
        blocksAllowedToMine.add(Blocks.chest);
        blocksAllowedToMine.add(Blocks.trapped_chest);

        if(MightyMiner.config.powMineGemstone){
            blocksAllowedToMine.add(Blocks.stained_glass_pane);
            blocksAllowedToMine.add(Blocks.stained_glass);
        }


    }

    @Override
    public void onDisable() {
        HardstoneAura.autoHardStone = false;
        mineBaritone.disableBaritone();
        KeybindHandler.resetKeybindState();
    }

    @Override
    public void onTick(TickEvent.Phase phase){
        mineBaritone.onTickEvent(phase);
        if(phase != TickEvent.Phase.START)
            return;

        if(PlayerUtils.isNearPlayer()){
            PlayerUtils.warpBackToIsland();
            MacroHandler.disableScript();
            HardstoneAura.autoHardStone = false;
            return;
        }

        if(rotation.rotating){
            KeybindHandler.resetKeybindState();
            return;
        }


        updateState();

        if(centering) {
            if(centerToBlockTick == 0) {
                KeybindHandler.resetKeybindState();
                centerToBlockTick = 20;
            }

            if(centerToBlockTick == 10)
                PlayerUtils.centerToBlock();

            centerToBlockTick --;

            if(centerToBlockTick == 0)
                centering = false;
            return;
        }


        System.out.println(currentState + " " + treasureState);

        switch (currentState){
            case TREASURE:
                HardstoneAura.autoHardStone = true;
                switch(treasureState){
                    case NONE: case SOLVING:
                        KeybindHandler.resetKeybindState();
                        break;
                    case WALKING:
                        rotation.intLockAngle(AngleUtils.getRequiredYaw(chest), 0, 1);
                        KeybindHandler.setKeyBindState(KeybindHandler.keybindW, true);
                        KeybindHandler.setKeyBindState(KeybindHandler.keyBindJump, true);
                        break;
                    case RETURNING:
                        if(!mineBaritone.isEnabled() && !BlockUtils.getPlayerLoc().equals(returnBlockPos)) {
                            mineBaritone.enableBaritone(returnBlockPos);
                        } else if(!mineBaritone.isEnabled() && BlockUtils.getPlayerLoc().equals(returnBlockPos)){
                            if (MightyMiner.config.powCenter)
                                centering = true;
                            currentState = treasureCacheState;
                        }
                        break;

                }
                break;

            case NORMAL: case UTurn:
                if(MightyMiner.config.powStoneAura) {
                    HardstoneAura.autoHardStone = !frontShouldMineSlow();
                    rotation.intLockAngle(playerYaw, (shouldLookDown() ? 60 : (frontShouldMineSlow() ? 27 : 0)), 200);
                } else
                    rotation.intLockAngle(playerYaw, (shouldLookDown() ? 60 : 27), 200);

                KeybindHandler.setKeyBindState(KeybindHandler.keybindW, true);
                KeybindHandler.setKeyBindState(KeybindHandler.keybindAttack, mc.objectMouseOver != null && mc.objectMouseOver.getBlockPos() != null && mc.objectMouseOver.getBlockPos().getY() >= (int)mc.thePlayer.posY);
                break;
        }

        if(rotation.rotating){
            KeybindHandler.resetKeybindState();
        }

    }

    private void updateState(){
        switch (currentState) {
            case TREASURE:
                if(System.currentTimeMillis() - treasureInitialTime > 7000 && treasureState != TreasureState.RETURNING) {
                    treasureState = TreasureState.RETURNING;
                    LogUtils.debugLog("Completed treasure due to timeout");
                }
                switch (treasureState){
                    case WALKING:
                        if (MathUtils.getDistanceBetweenTwoBlock(BlockUtils.getPlayerLoc(), chest) < 3f)
                            treasureState = TreasureState.SOLVING;
                        break;
                }
                return;
            case NORMAL:
                if(!blocksAllowedToMine.contains(BlockUtils.getRelativeBlock(0, 0, 1)) || !blocksAllowedToMine.contains(BlockUtils.getRelativeBlock(0, 1, 1))) {
                    turnState = 1 - turnState;
                    playerYaw = AngleUtils.get360RotationYaw(playerYaw + getRotAmount());
                    uTurnCachePos = BlockUtils.getPlayerLoc();
                    currentState = State.UTurn;
                    KeybindHandler.resetKeybindState();
                }
                break;
            case UTurn:
                if (MathUtils.getDistanceBetweenTwoBlock(BlockUtils.getPlayerLoc(), uTurnCachePos) > 4) {
                    playerYaw = AngleUtils.get360RotationYaw(playerYaw + getRotAmount());
                    if (MightyMiner.config.powCenter)
                        centering = true;
                    currentState = State.NORMAL;
                }
                break;
        }
    }

    @Override
    public void onLastRender() {
        mineBaritone.onRenderEvent();

        if(rotation.rotating)
            rotation.update();
    }

    @Override
    public void onOverlayRenderEvent(RenderGameOverlayEvent event) {
        mineBaritone.onOverlayRenderEvent(event);
    }

    @Override
    public void onMessageReceived(String message){
        if(message.contains("You have successfully picked the lock on this chest")){
          //  currentState = treasureCacheState;
            treasureState = TreasureState.RETURNING;
            LogUtils.debugLog("Completed treasure");
        }
        if(message.contains("You uncovered a treasure chest!")){
            if(currentState != State.TREASURE)
                treasureCacheState = currentState;
            currentState = State.TREASURE;
            treasureState = TreasureState.NONE;
            returnBlockPos = BlockUtils.getPlayerLoc();
            new Thread(() -> {
                try{
                    Thread.sleep(350); // Hypickle lag
                    LogUtils.debugLog("Starting to find ");
                    chest = BlockUtils.findBlock(16, Blocks.chest, Blocks.trapped_chest).get(0);
                    treasureState = MathUtils.getDistanceBetweenTwoBlock(BlockUtils.getPlayerLoc(), chest) > 3f ? TreasureState.WALKING : TreasureState.SOLVING;
                    for(int i = 0; i < 5; i++){
                        if(BlockUtils.getRelativeBlockPos(0, 0, i).equals(chest) || BlockUtils.getRelativeBlockPos(0, 1, i).equals(chest)){
                            returnBlockPos = BlockUtils.getRelativeBlockPos(0, (float)mc.thePlayer.posY - chest.getY(), i + 1);
                        }
                    }


                }
                catch (Exception ignored){}
            }).start();


            KeybindHandler.resetKeybindState();
            treasureInitialTime = System.currentTimeMillis();
        }
    }

    @Override
    public void onPacketReceived(Packet<?> packet){
        if(currentState == State.TREASURE && treasureState == TreasureState.SOLVING && treasureInitialTime > 200 && packet instanceof S2APacketParticles && chest != null){
            if(((S2APacketParticles) packet).getParticleType() == EnumParticleTypes.CRIT){
                try {
                    if(Math.abs((((S2APacketParticles) packet).getXCoordinate()) - chest.getX()) < 2 && Math.abs((((S2APacketParticles) packet).getYCoordinate()) - chest.getY()) < 2 && Math.abs((((S2APacketParticles) packet).getZCoordinate()) - chest.getZ()) < 2) {
                        rotation.intLockAngle(
                                AngleUtils.getRequiredYaw(((S2APacketParticles) packet).getXCoordinate() - mc.thePlayer.posX, ((S2APacketParticles) packet).getZCoordinate() - mc.thePlayer.posZ),
                                AngleUtils.getRequiredPitch(((S2APacketParticles) packet).getXCoordinate() - mc.thePlayer.posX, (((S2APacketParticles) packet).getYCoordinate()) - (mc.thePlayer.posY + 1.62d), ((S2APacketParticles) packet).getZCoordinate() - mc.thePlayer.posZ),
                                50);
                    }
                }catch (Exception ignored){}
            }
        }
    }

    int getRotAmount(){
        //check blacklisted blocks
        if(!(blocksAllowedToMine.contains(BlockUtils.getRelativeBlock(-1, 0, 0))) || !(blocksAllowedToMine.contains(BlockUtils.getRelativeBlock(1, 0, 0)))
                || !(blocksAllowedToMine.contains(BlockUtils.getRelativeBlock(-1, 1, 0))) || !(blocksAllowedToMine.contains(BlockUtils.getRelativeBlock(1, 1, 0)))){

            //check which side is possible to walk, if none, 180
            return  (blocksAllowedToMine.contains(BlockUtils.getRelativeBlock(-1, 0, 0)) && blocksAllowedToMine.contains(BlockUtils.getRelativeBlock(-1, 1, 0))) ? (-90)
                    : (blocksAllowedToMine.contains(BlockUtils.getRelativeBlock(1, 0, 0)) && blocksAllowedToMine.contains(BlockUtils.getRelativeBlock(1, 1, 0)) ? 90 : 180);
        } else
            return turnState == 1 ? 90 : -90;
        // both sides can be walked, oscillate between 90 and -90 to increase area mined
    }
    boolean shouldLookDown(){
        return (AngleUtils.shouldLookAtCenter(BlockUtils.getRelativeBlockPos(0, 0, 1)) && BlockUtils.isPassable(BlockUtils.getRelativeBlock(0, 1, 1)))
        || (AngleUtils.shouldLookAtCenter(BlockUtils.getRelativeBlockPos(0, 0, 0)) && BlockUtils.isPassable(BlockUtils.getRelativeBlock(0, 1, 0)));
    }
    boolean frontShouldMineSlow(){
        return mineSlowBlocks.contains(BlockUtils.getRelativeBlock(0, 0, 1)) || mineSlowBlocks.contains(BlockUtils.getRelativeBlock(0, 1, 1))
                || BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.stained_glass_pane) ||  BlockUtils.getRelativeBlock(0, 1, 0).equals(Blocks.stained_glass_pane) ;
    }


    MineBehaviour getAutomineConfig(){
        return new MineBehaviour(
                AutoMineType.DYNAMIC,
                false,
                true,
                false,
                250,
                8,
                new ArrayList<Block>(){
                    {
                        add(Blocks.chest);
                        add(Blocks.trapped_chest);
                    }
                },
                null,
                256,
                0
        );
    }
}
