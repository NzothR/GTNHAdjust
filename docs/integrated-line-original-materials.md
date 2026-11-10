# 原始材料方案备份

以下为结构测试版 `d5f8489` 的主机和模块结构映射原文，供未来设计等级变体参考。它们不参与当前结构验证；对应的原始 `.mbs` 数据仍保留在项目资源中。

## 主机

```java
    @Override
    public IStructureDefinition<IntegratedLine> getStructureDefinition() {
        if (structure == null) {
            StructureDefinition.Builder<IntegratedLine> b = StructureDefinition.<IntegratedLine>builder();
            for (String piece : PIECES) b.addShape(
                piece,
                StructureUtility
                    .transpose(StructureUtils.readStructureFromFile("gtnhadjust:multiblock/integrated_line/" + piece)));
            structure = b.addElement('A', Casings.SingularityReinforcedStellarShieldingCasing.asElement())
                .addElement('B', StructureUtility.ofBlock(Loaders.componentAssemblylineCasing, 12))
                .addElement('C', Casings.DimensionalInjectionCasing.asElement())
                .addElement('D', StructureUtility.ofBlock(GregTechAPI.sBlockCasingsSEMotor, 4))
                .addElement('E', Casings.ExtremeDensitySpaceBendingCasing.asElement())
                .addElement('F', Casings.NaquadriaReinforcedWaterPlantCasing.asElement())
                .addElement('G', Casings.CelestialMatterGuidanceCasing.asElement())
                .addElement('H', Casings.DimensionalBridge.asElement())
                .addElement('I', Casings.SpaceTimeBendingCore.asElement())
                .addElement('J', Casings.ParticleBeamGuidancePipeCasing.asElement())
                .addElement('K', GTStructureUtility.ofFrame(Materials.NaquadahAlloy))
                .addElement('L', Casings.OmniPurposeInfinityFusedGlass.asElement())
                .addElement('M', StructureUtility.ofBlock(ItemRegistry.bw_realglas, 6))
                .addElement(
                    'N',
                    GTStructureUtility.buildHatchAdder(IntegratedLine.class)
                        .anyOf(
                            HatchElement.InputBus,
                            HatchElement.OutputBus,
                            HatchElement.InputHatch,
                            HatchElement.OutputHatch)
                        .casingIndex(961)
                        .hint(1)
                        .buildAndChain(Casings.CelestialMatterGuidanceCasing.asElement()))
                .addElement('O', Casings.QuarkExclusionCasing.asElement())
                .addElement('P', Casings.ReinforcedTemporalStructureCasing.asElement())
                .addElement('Q', Casings.CentralGravitonFlowModulator.asElement())
                .addElement('R', Casings.StellarEnergySiphonCasing.asElement())
                .addElement('S', Casings.HarmonicPhononTransmissionConduit.asElement())
                .addElement('T', StructureUtility.ofBlock(Loaders.gravityStabilizationCasing, 0))
                .addElement('U', Casings.GallifreyanSpacetimeCompressionFieldGenerator.asElement())
                .addElement('V', Casings.GallifreyanTimeDilationFieldGenerator.asElement())
                .addElement('W', Casings.ReinforcedSpatialStructureCasing.asElement())
                .addElement('X', Casings.GallifreyanStabilisationFieldGenerator.asElement())
                .addElement(
                    'Y',
                    HatchElementBuilder.<IntegratedLine>builder()
                        .anyOf(ModuleElement.Module)
                        .casingIndex(960)
                        .hint(1)
                        .buildAndChain(TTCasingsContainer.GodforgeCasings, 0))
                .addElement('Z', Casings.DimensionalBridge.asElement())
                .addElement('a', StructureUtility.isAir())
                .build();
        }
        return structure;
    }

    public boolean addModule(IGregTechTileEntity tile, int casing) {
        if (tile != null && tile.getMetaTileEntity() instanceof IntegratedModule module && !modules.contains(module))
            return modules.add(module);
        return false;
    }

    private void disconnect() {
        for (IntegratedModule module : modules) module.setConnected(false);
        modules.clear();
    }

```

## 联合模块

```java
    @Override
    public IStructureDefinition<IntegratedModule> getStructureDefinition() {
        if (structure == null) structure = StructureDefinition.<IntegratedModule>builder()
            .addShape(
                "main",
                StructureUtility
                    .transpose(StructureUtils.readStructureFromFile("gtnhadjust:multiblock/integrated_line/module")))
            .addElement('A', StructureUtility.ofBlock(ItemRegistry.bw_realglas, 6))
            .addElement('B', StructureUtility.ofBlock(Loaders.componentAssemblylineCasing, 12))
            .addElement('C', Casings.DimensionalInjectionCasing.asElement())
            .addElement('D', Casings.DimensionalBridge.asElement())
            .addElement('E', Casings.ExtremeDensitySpaceBendingCasing.asElement())
            .addElement('F', Casings.ParticleBeamGuidancePipeCasing.asElement())
            .addElement('G', StructureUtility.ofBlock(GregTechAPI.sBlockCasingsSEMotor, 4))
            .addElement('H', GTStructureUtility.ofFrame(Materials.NaquadahAlloy))
            .addElement('I', Casings.SingularityReinforcedStellarShieldingCasing.asElement())
            .addElement('J', Casings.CelestialMatterGuidanceCasing.asElement())
            .addElement(
                'K',
                StructureUtility.ofChain(
                    StructureUtility.isAir(),
                    GTStructureUtility.buildHatchAdder(IntegratedModule.class)
                        .anyOf(
                            HatchElement.InputBus,
                            HatchElement.OutputBus,
                            HatchElement.InputHatch,
                            HatchElement.OutputHatch)
                        .casingIndex(960)
                        .hint(1)
                        .buildAndChain(Casings.SingularityReinforcedStellarShieldingCasing.asElement())))
            .build();
        return structure;
    }

```
