-- Esquema exigido pelo shedlock-provider-jdbc-template: garante que cada
-- scheduler rode em uma única instância do bootstrap por vez.
CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);
