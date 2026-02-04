package com.stardevllc.staritemgenerators.common.model;

import com.stardevllc.starlib.clock.ClockManager;
import com.stardevllc.starlib.injector.FieldInjector;
import com.stardevllc.starlib.injector.SimpleFieldInjector;
import com.stardevllc.starlib.objects.registry.Registry;
import com.stardevllc.starlib.objects.registry.RegistryObject;

public class GeneratorRegistry extends Registry<String, ItemGenerator> {
    
    private final ClockManager clockManager;
    
    private final FieldInjector injector;
    
    public GeneratorRegistry(ClockManager clockManager) {
        super(ItemGenerator::getId);
        this.clockManager = clockManager;
        
        this.injector = new SimpleFieldInjector();
        this.injector.set(this);
        this.injector.set(clockManager);
    }
    
    @Override
    public RegistryObject<String, ItemGenerator> register(RegistryObject<String, ItemGenerator> registryObject) {
        injector.inject(registryObject.get());
        return super.register(registryObject);
    }
    
    @Override
    public RegistryObject<String, ItemGenerator> register(String key, ItemGenerator value) {
        injector.inject(value);
        return super.register(key, value);
    }
    
    public ClockManager getClockManager() {
        return clockManager;
    }
}