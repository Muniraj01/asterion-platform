CREATE TABLE merchant (
                          merchant_id UUID PRIMARY KEY,
                          owner_user_id UUID NOT NULL,
                          business_name VARCHAR(255) NOT NULL,
                          legal_name VARCHAR(255) NOT NULL,
                          contact_email VARCHAR(255) NOT NULL,
                          status VARCHAR(50) NOT NULL,
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL
);