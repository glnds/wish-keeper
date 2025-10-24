CREATE TABLE wishes (
    id VARCHAR(36) PRIMARY KEY,
    productName VARCHAR(120) NOT NULL,
    quantity INTEGER NOT NULL,
    beneficiaryId INTEGER NOT NULL,
    CONSTRAINT fk_beneficiary FOREIGN KEY (beneficiaryId)
    REFERENCES people (id)
    ON DELETE CASCADE
);