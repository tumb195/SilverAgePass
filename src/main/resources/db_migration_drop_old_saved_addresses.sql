-- Database Migration: Drop Old user_saved_addresses Table
-- Description: Removes the deprecated user_saved_addresses table after migrating to user_addresses
-- Note: Run this ONLY after confirming all data has been migrated to user_addresses table

-- Step 1: (Optional) Migrate existing data to new table if not already done
-- INSERT INTO user_addresses (user_id, address, is_default, create_time, update_time)
-- SELECT user_id, address, FALSE, NOW(), NOW()
-- FROM user_saved_addresses
-- WHERE NOT EXISTS (
--     SELECT 1 FROM user_addresses ua 
--     WHERE ua.user_id = user_saved_addresses.user_id 
--     AND ua.address = user_saved_addresses.address
-- );

-- Step 2: Drop the old table
DROP TABLE IF EXISTS `user_saved_addresses`;
