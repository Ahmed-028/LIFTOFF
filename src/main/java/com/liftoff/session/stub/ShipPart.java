package com.liftoff.session.stub;

import com.liftoff.model.SourceRef;
import com.liftoff.model.ValueBasis;

import java.util.List;


public record ShipPart(String id, ModuleSlot slot, String name, String kidName, String description,
                       String basedOn,
                       List<String> missions,
                       double massKg,
                       double powerSupplyW,
                       double powerDrawW,
                       double payloadMassKg,
                       double bayLengthM, double bayDiameterM,
                       double lengthM, double diameterM,
                       double thrustN, double specificImpulseS,
                       double propellantCapacityKg,
                       double dataGbitPerDay,
                       double costMusd, ValueBasis costBasis,
                       String modelId,
                       SourceRef source) {
    public ShipPart {
        if (missions != null) {
            missions = List.copyOf(missions);
        }
    }
}
