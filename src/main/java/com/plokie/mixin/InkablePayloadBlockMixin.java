package com.plokie.mixin;

import com.plokie.interfaces.IInkablePayloadBlock;
import com.plokie.interfaces.IPlayerTeamMixin;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

@Mixin(Slime.class)
public class InkablePayloadBlockMixin implements IInkablePayloadBlock {
    @Unique
    IPlayerTeamMixin team = null;
    @Unique
    UUID blockDisplayUUID = null;

    @Override
    public void setLinkedBlockDisplay(UUID blockDisplayUUID) {
        this.blockDisplayUUID = blockDisplayUUID;
    }

    @Override
    public IPlayerTeamMixin getTeam() {
        return team;
    }

    @Override
    public void setTeam(IPlayerTeamMixin team) {
        this.team = team;

        Block wallBlock = Blocks.WHITE_WOOL;
        if(team != null) {
            wallBlock = team.getWallBlock();
        }

        if(blockDisplayUUID != null) {
            Slime self = (Slime)(Object)this;

            Entity blockDisplayEntity = self.level().getEntity(blockDisplayUUID);
            if(blockDisplayEntity != null) {
                if(blockDisplayEntity instanceof Display.BlockDisplay blockDisplay) {
                    blockDisplay.setBlockState(wallBlock.defaultBlockState());
                }
            }
        }
    }

    @Inject(method="addAdditionalSaveData", at=@At("TAIL"))
    void onSaveData(ValueOutput valueOutput, CallbackInfo ci)
    {
        if(blockDisplayUUID != null) {
            valueOutput.putString("linkedBlockDisplay", blockDisplayUUID.toString());
        }
    }

    @Inject(method="readAdditionalSaveData", at=@At("TAIL"))
    void onReadData(ValueInput valueInput, CallbackInfo ci)
    {
        Optional<String> linkedUUID = valueInput.getString("linkedBlockDisplay");

        if(linkedUUID.isPresent())
        {
            blockDisplayUUID = UUID.fromString(linkedUUID.get());
        }
    }
}
