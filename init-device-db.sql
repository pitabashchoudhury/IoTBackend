-- ============================================
-- device_db: Schema + Seed Data
-- ============================================

-- Devices table
CREATE TABLE IF NOT EXISTS devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    address VARCHAR(500),
    location_label VARCHAR(255),
    mqtt_topic_prefix VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Device controls table
CREATE TABLE IF NOT EXISTS device_controls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    control_type VARCHAR(50) NOT NULL,
    current_value VARCHAR(255),
    min_value FLOAT,
    max_value FLOAT,
    step FLOAT,
    options TEXT,
    mqtt_topic VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_devices_user_id ON devices(user_id);
CREATE INDEX IF NOT EXISTS idx_device_controls_device_id ON device_controls(device_id);

-- ============================================
-- Seed Data (user_id references auth_db users)
-- ============================================

-- Pitabash's devices
INSERT INTO devices (id, user_id, name, type, is_online, latitude, longitude, address, location_label, mqtt_topic_prefix, created_at, updated_at) VALUES
    ('11111111-1111-1111-1111-111111111111', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Living Room Light', 'LIGHT', true, 20.2961, 85.8245, 'Bhubaneswar, Odisha', 'Living Room', 'home/living-room', NOW(), NOW()),
    ('22222222-2222-2222-2222-222222222222', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Bedroom AC', 'THERMOSTAT', true, 20.2961, 85.8245, 'Bhubaneswar, Odisha', 'Bedroom', 'home/bedroom', NOW(), NOW()),
    ('33333333-3333-3333-3333-333333333333', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Front Door Camera', 'CAMERA', false, 20.2961, 85.8245, 'Bhubaneswar, Odisha', 'Entrance', 'home/entrance', NOW(), NOW()),
    ('44444444-4444-4444-4444-444444444444', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Kitchen Fan', 'FAN', true, 20.2961, 85.8245, 'Bhubaneswar, Odisha', 'Kitchen', 'home/kitchen', NOW(), NOW()),
    ('55555555-5555-5555-5555-555555555555', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Front Door Lock', 'LOCK', true, 20.2961, 85.8245, 'Bhubaneswar, Odisha', 'Entrance', 'home/entrance', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- John's devices
INSERT INTO devices (id, user_id, name, type, is_online, latitude, longitude, address, location_label, mqtt_topic_prefix, created_at, updated_at) VALUES
    ('66666666-6666-6666-6666-666666666666', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', 'Office Light', 'LIGHT', true, 28.6139, 77.2090, 'New Delhi', 'Office', 'office/main', NOW(), NOW()),
    ('77777777-7777-7777-7777-777777777777', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', 'Temperature Sensor', 'SENSOR', true, 28.6139, 77.2090, 'New Delhi', 'Office', 'office/sensor', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Jane's devices
INSERT INTO devices (id, user_id, name, type, is_online, latitude, longitude, address, location_label, mqtt_topic_prefix, created_at, updated_at) VALUES
    ('88888888-8888-8888-8888-888888888888', 'c3d4e5f6-a7b8-9012-cdef-123456789012', 'Garden Sprinkler', 'SWITCH', false, 19.0760, 72.8777, 'Mumbai', 'Garden', 'garden/sprinkler', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- Device Controls
-- ============================================

-- Living Room Light controls
INSERT INTO device_controls (id, device_id, name, control_type, current_value, min_value, max_value, step, options, mqtt_topic) VALUES
    ('aaaa1111-aaaa-1111-aaaa-111111111111', '11111111-1111-1111-1111-111111111111', 'Power', 'TOGGLE', 'true', NULL, NULL, NULL, '[]', 'home/living-room/power'),
    ('aaaa2222-aaaa-2222-aaaa-222222222222', '11111111-1111-1111-1111-111111111111', 'Brightness', 'SLIDER', '75', 0, 100, 1, '[]', 'home/living-room/brightness'),
    ('aaaa3333-aaaa-3333-aaaa-333333333333', '11111111-1111-1111-1111-111111111111', 'Color', 'COLOR_PICKER', '#FFFFFF', NULL, NULL, NULL, '[]', 'home/living-room/color')
ON CONFLICT (id) DO NOTHING;

-- Bedroom AC controls
INSERT INTO device_controls (id, device_id, name, control_type, current_value, min_value, max_value, step, options, mqtt_topic) VALUES
    ('bbbb1111-bbbb-1111-bbbb-111111111111', '22222222-2222-2222-2222-222222222222', 'Power', 'TOGGLE', 'true', NULL, NULL, NULL, '[]', 'home/bedroom/power'),
    ('bbbb2222-bbbb-2222-bbbb-222222222222', '22222222-2222-2222-2222-222222222222', 'Temperature', 'SLIDER', '24', 16, 30, 0.5, '[]', 'home/bedroom/temperature'),
    ('bbbb3333-bbbb-3333-bbbb-333333333333', '22222222-2222-2222-2222-222222222222', 'Mode', 'DROPDOWN', 'Cool', NULL, NULL, NULL, '["Cool","Heat","Auto","Dry","Fan"]', 'home/bedroom/mode')
ON CONFLICT (id) DO NOTHING;

-- Front Door Camera controls
INSERT INTO device_controls (id, device_id, name, control_type, current_value, min_value, max_value, step, options, mqtt_topic) VALUES
    ('cccc1111-cccc-1111-cccc-111111111111', '33333333-3333-3333-3333-333333333333', 'Recording', 'TOGGLE', 'false', NULL, NULL, NULL, '[]', 'home/entrance/recording'),
    ('cccc2222-cccc-2222-cccc-222222222222', '33333333-3333-3333-3333-333333333333', 'Night Vision', 'TOGGLE', 'true', NULL, NULL, NULL, '[]', 'home/entrance/night-vision')
ON CONFLICT (id) DO NOTHING;

-- Kitchen Fan controls
INSERT INTO device_controls (id, device_id, name, control_type, current_value, min_value, max_value, step, options, mqtt_topic) VALUES
    ('dddd1111-dddd-1111-dddd-111111111111', '44444444-4444-4444-4444-444444444444', 'Power', 'TOGGLE', 'true', NULL, NULL, NULL, '[]', 'home/kitchen/power'),
    ('dddd2222-dddd-2222-dddd-222222222222', '44444444-4444-4444-4444-444444444444', 'Speed', 'SLIDER', '3', 1, 5, 1, '[]', 'home/kitchen/speed')
ON CONFLICT (id) DO NOTHING;

-- Front Door Lock controls
INSERT INTO device_controls (id, device_id, name, control_type, current_value, min_value, max_value, step, options, mqtt_topic) VALUES
    ('eeee1111-eeee-1111-eeee-111111111111', '55555555-5555-5555-5555-555555555555', 'Lock', 'TOGGLE', 'true', NULL, NULL, NULL, '[]', 'home/entrance/lock'),
    ('eeee2222-eeee-2222-eeee-222222222222', '55555555-5555-5555-5555-555555555555', 'Auto Lock', 'TOGGLE', 'false', NULL, NULL, NULL, '[]', 'home/entrance/auto-lock')
ON CONFLICT (id) DO NOTHING;

-- Office Light controls
INSERT INTO device_controls (id, device_id, name, control_type, current_value, min_value, max_value, step, options, mqtt_topic) VALUES
    ('ffff1111-ffff-1111-ffff-111111111111', '66666666-6666-6666-6666-666666666666', 'Power', 'TOGGLE', 'true', NULL, NULL, NULL, '[]', 'office/main/power'),
    ('ffff2222-ffff-2222-ffff-222222222222', '66666666-6666-6666-6666-666666666666', 'Brightness', 'SLIDER', '100', 0, 100, 5, '[]', 'office/main/brightness')
ON CONFLICT (id) DO NOTHING;

-- Temperature Sensor controls
INSERT INTO device_controls (id, device_id, name, control_type, current_value, min_value, max_value, step, options, mqtt_topic) VALUES
    ('aaab1111-aaab-1111-aaab-111111111111', '77777777-7777-7777-7777-777777777777', 'Reading', 'BUTTON', '28.5', NULL, NULL, NULL, '[]', 'office/sensor/reading')
ON CONFLICT (id) DO NOTHING;

-- Garden Sprinkler controls
INSERT INTO device_controls (id, device_id, name, control_type, current_value, min_value, max_value, step, options, mqtt_topic) VALUES
    ('aaac1111-aaac-1111-aaac-111111111111', '88888888-8888-8888-8888-888888888888', 'Power', 'TOGGLE', 'false', NULL, NULL, NULL, '[]', 'garden/sprinkler/power'),
    ('aaac2222-aaac-2222-aaac-222222222222', '88888888-8888-8888-8888-888888888888', 'Duration', 'SLIDER', '30', 5, 120, 5, '[]', 'garden/sprinkler/duration')
ON CONFLICT (id) DO NOTHING;

-- Verify
SELECT 'device_db seeded successfully' AS status;
SELECT 'Devices:' AS info, count(*) AS count FROM devices;
SELECT 'Controls:' AS info, count(*) AS count FROM device_controls;
