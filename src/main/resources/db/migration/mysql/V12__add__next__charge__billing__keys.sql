ALTER TABLE billing_keys
    ADD COLUMN next_charge_date DATETIME(6) NULL;

CREATE INDEX idx_billing_keys_active_next_charge_date
    ON billing_keys (active, next_charge_date);