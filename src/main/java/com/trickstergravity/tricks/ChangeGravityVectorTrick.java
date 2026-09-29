package com.trickstergravity.tricks;

import dev.enjarai.trickster.Trickster;
import dev.enjarai.trickster.spell.EvaluationResult;
import dev.enjarai.trickster.spell.Fragment;
import dev.enjarai.trickster.spell.Pattern;
import dev.enjarai.trickster.spell.SpellContext;
import dev.enjarai.trickster.spell.exception.TricksterEngineException;
import dev.enjarai.trickster.spell.exception.blunder.NotEnoughManaBlunder;
import dev.enjarai.trickster.spell.exception.blunder.UnknownEntityBlunder;
import dev.enjarai.trickster.spell.execution.TickData;
import dev.enjarai.trickster.spell.fragment.*;
import dev.enjarai.trickster.spell.mana.storage.ManaVariant;
import dev.enjarai.trickster.spell.mana.type.Manae;
import dev.enjarai.trickster.spell.mana.type.component.ManaComponents;
import dev.enjarai.trickster.spell.trick.Trick;
import dev.enjarai.trickster.spell.type.Signature;
import gravity_changer.api.GravityChangerAPI;
import net.minecraft.entity.projectile.AbstractWindChargeEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3dc;

import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

public class ChangeGravityVectorTrick extends Trick<ChangeGravityVectorTrick> {

    private static final TickData.Key<HashMap<EntityFragment, Float>> COMPOUND_LEN = new TickData.Key<>(
            Trickster.id("impulse_compound_len"), null
    );

    public ChangeGravityVectorTrick() {
        super(
                Pattern.of(1, 4, 7, 6, 8, 7),
                Signature.of(FragmentType.ENTITY, FragmentType.VECTOR, ChangeGravityVectorTrick::run, FragmentType.ENTITY)
        );
    }

    public Direction vectorToDirection(VectorFragment v) {
        Vec3d current = new Vec3d(v.x(), v.y(), v.z());

        List<Vec3d> vectors = List.of(
            new Vec3d(0, 1, 0),
            new Vec3d(0, -1, 0),
            new Vec3d(0, 0, -1),
            new Vec3d(0, 0, 1),
            new Vec3d(1, 0, 0),
            new Vec3d(-1, 0, 0)
        );
        List<Direction> directions = List.of(
                Direction.UP,
                Direction.DOWN,
                Direction.NORTH,
                Direction.SOUTH,
                Direction.EAST,
                Direction.WEST
        );
        int mn = 0;
        for (int i = 1; i < 6; i++) {
            if (current.distanceTo(vectors.get(i)) < current.distanceTo(vectors.get(mn))) {
                mn = i;
            }
        }
        return directions.get(mn);
    }

    public EntityFragment run(SpellContext ctx, EntityFragment target, VectorFragment v) {
        var entity = target
                .getEntity(ctx)
                .orElseThrow(UnknownEntityBlunder::new);
        var direction = vectorToDirection(v);

        //var map = COMPOUND_LEN.set(ctx.data(), COMPOUND_LEN.get(ctx.data()).orElse(new HashMap<>()));
        //var length = (float) vector.length() + map.getOrDefault(target, 0f);
        var cost = 60;
        if (direction == Direction.DOWN) {
            cost = 0;
        }

        try {
            Predicate<ManaVariant> filter = variant -> variant.isOf(Manae.STORM) && 0.5 < variant.getComponentMap().get(ManaComponents.STABILITY).getStability(ctx.source().getWorld());
            ctx.checkScaledMana(filter, cost);
            ctx.useScaledMana(filter, cost);
        } catch (NotEnoughManaBlunder blunder) {
            ctx.useScaledMana(cost);
        }

        //map.put(target, length);

        GravityChangerAPI.setBaseGravityDirection(entity, direction);
        entity.limitFallDistance();
        //entity.velocityModified = true;

        return target;
    }
}
