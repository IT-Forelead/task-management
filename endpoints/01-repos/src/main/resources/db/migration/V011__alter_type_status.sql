DO $$
BEGIN
    IF 'expired' NOT IN (SELECT unnest(enum_range(NULL::STATUS))) THEN
        ALTER TYPE STATUS ADD VALUE 'expired';
    END IF;
END $$;