-- V1__initial_schema.sql
-- MySQL 8 / InnoDB / utf8mb4_0900_ai_ci
--
-- Schema for the barber appointment application.
-- Times are stored as wall-clock DATETIME in the shop's local timezone
-- (see AvailabilityService); the audit columns are UTC Instants.

-- =========================
-- Security / Auth tables
-- =========================
CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,

    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name),

    INDEX idx_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE capabilities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255) NULL,

    CONSTRAINT pk_capabilities PRIMARY KEY (id),
    CONSTRAINT uk_capabilities_name UNIQUE (name),

    INDEX idx_capabilities_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE roles_capabilities (
    role_id BIGINT NOT NULL,
    capability_id BIGINT NOT NULL,

    CONSTRAINT pk_roles_capabilities PRIMARY KEY (role_id, capability_id),

    CONSTRAINT fk_roles_capabilities_role
        FOREIGN KEY (role_id) REFERENCES roles(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_roles_capabilities_capability
        FOREIGN KEY (capability_id) REFERENCES capabilities(id)
        ON DELETE CASCADE,

    INDEX idx_roles_capabilities_capability_id (capability_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,

    uuid BINARY(16) NOT NULL,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    firstname VARCHAR(255) NOT NULL,
    lastname VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NULL,
    role_id BIGINT NOT NULL,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at DATETIME NULL,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_uuid UNIQUE (uuid),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),

    CONSTRAINT fk_users_role
        FOREIGN KEY (role_id) REFERENCES roles(id)
        ON DELETE RESTRICT,

    INDEX ix_users_role_id (role_id),
    INDEX ix_users_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =========================
-- Domain tables
-- =========================

-- The freelance barber. Modelled as an entity (rather than assumed) so the
-- schedule, services and promotions all hang off an explicit aggregate root.
CREATE TABLE barbers (
    id BIGINT NOT NULL AUTO_INCREMENT,

    uuid BINARY(16) NOT NULL,
    user_id BIGINT NOT NULL,
    shop_name VARCHAR(255) NOT NULL,
    bio TEXT NULL,
    address VARCHAR(255) NULL,
    photo_url VARCHAR(512) NULL,
    slot_step_minutes INT NOT NULL DEFAULT 15,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at DATETIME NULL,

    CONSTRAINT pk_barbers PRIMARY KEY (id),
    CONSTRAINT uk_barbers_uuid UNIQUE (uuid),
    CONSTRAINT uk_barbers_user_id UNIQUE (user_id),

    CONSTRAINT fk_barbers_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE RESTRICT,

    INDEX ix_barbers_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE customers (
    id BIGINT NOT NULL AUTO_INCREMENT,

    uuid BINARY(16) NOT NULL,
    user_id BIGINT NOT NULL,
    notes VARCHAR(1000) NULL,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at DATETIME NULL,

    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uk_customers_uuid UNIQUE (uuid),
    CONSTRAINT uk_customers_user_id UNIQUE (user_id),

    CONSTRAINT fk_customers_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE RESTRICT,

    INDEX ix_customers_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- A bookable offering: "Haircut", 30 min, 15.00 EUR.
CREATE TABLE barber_services (
    id BIGINT NOT NULL AUTO_INCREMENT,

    uuid BINARY(16) NOT NULL,
    barber_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NULL,
    duration_minutes INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at DATETIME NULL,

    CONSTRAINT pk_barber_services PRIMARY KEY (id),
    CONSTRAINT uk_barber_services_uuid UNIQUE (uuid),
    CONSTRAINT uk_barber_services_barber_name UNIQUE (barber_id, name),

    CONSTRAINT fk_barber_services_barber
        FOREIGN KEY (barber_id) REFERENCES barbers(id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_barber_services_duration CHECK (duration_minutes > 0),
    CONSTRAINT ck_barber_services_price CHECK (price >= 0),

    INDEX ix_barber_services_barber_id (barber_id),
    INDEX ix_barber_services_active (active),
    INDEX ix_barber_services_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- The recurring weekly template the barber works to.
CREATE TABLE working_hours (
    id BIGINT NOT NULL AUTO_INCREMENT,

    barber_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at DATETIME NULL,

    CONSTRAINT pk_working_hours PRIMARY KEY (id),
    CONSTRAINT uk_working_hours_barber_day_start UNIQUE (barber_id, day_of_week, start_time),

    CONSTRAINT fk_working_hours_barber
        FOREIGN KEY (barber_id) REFERENCES barbers(id)
        ON DELETE CASCADE,

    CONSTRAINT ck_working_hours_range CHECK (end_time > start_time),

    INDEX ix_working_hours_barber_day (barber_id, day_of_week)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- One-off blocks carved out of the weekly template: holidays, appointments
-- at the dentist, an afternoon off.
CREATE TABLE time_off (
    id BIGINT NOT NULL AUTO_INCREMENT,

    uuid BINARY(16) NOT NULL,
    barber_id BIGINT NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    reason VARCHAR(255) NULL,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at DATETIME NULL,

    CONSTRAINT pk_time_off PRIMARY KEY (id),
    CONSTRAINT uk_time_off_uuid UNIQUE (uuid),

    CONSTRAINT fk_time_off_barber
        FOREIGN KEY (barber_id) REFERENCES barbers(id)
        ON DELETE CASCADE,

    CONSTRAINT ck_time_off_range CHECK (end_at > start_at),

    INDEX ix_time_off_barber_start (barber_id, start_at),
    INDEX ix_time_off_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- The promotion side of the brief: a seasonal discount the barber advertises
-- on the public landing page.
CREATE TABLE promotions (
    id BIGINT NOT NULL AUTO_INCREMENT,

    uuid BINARY(16) NOT NULL,
    barber_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NULL,
    discount_percent INT NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at DATETIME NULL,

    CONSTRAINT pk_promotions PRIMARY KEY (id),
    CONSTRAINT uk_promotions_uuid UNIQUE (uuid),

    CONSTRAINT fk_promotions_barber
        FOREIGN KEY (barber_id) REFERENCES barbers(id)
        ON DELETE CASCADE,

    CONSTRAINT ck_promotions_discount CHECK (discount_percent BETWEEN 1 AND 100),
    CONSTRAINT ck_promotions_range CHECK (valid_to >= valid_from),

    INDEX ix_promotions_barber_id (barber_id),
    INDEX ix_promotions_validity (valid_from, valid_to),
    INDEX ix_promotions_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE promotions_services (
    promotion_id BIGINT NOT NULL,
    barber_service_id BIGINT NOT NULL,

    CONSTRAINT pk_promotions_services PRIMARY KEY (promotion_id, barber_service_id),

    CONSTRAINT fk_promotions_services_promotion
        FOREIGN KEY (promotion_id) REFERENCES promotions(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_promotions_services_service
        FOREIGN KEY (barber_service_id) REFERENCES barber_services(id)
        ON DELETE CASCADE,

    INDEX idx_promotions_services_service_id (barber_service_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- The booking itself. end_at is derived from the service duration at save
-- time and persisted, so overlap queries stay a plain indexed range scan.
CREATE TABLE appointments (
    id BIGINT NOT NULL AUTO_INCREMENT,

    uuid BINARY(16) NOT NULL,
    customer_id BIGINT NOT NULL,
    barber_service_id BIGINT NOT NULL,
    promotion_id BIGINT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    price_charged DECIMAL(10,2) NOT NULL,
    notes VARCHAR(1000) NULL,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at DATETIME NULL,

    CONSTRAINT pk_appointments PRIMARY KEY (id),
    CONSTRAINT uk_appointments_uuid UNIQUE (uuid),

    CONSTRAINT fk_appointments_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_appointments_service
        FOREIGN KEY (barber_service_id) REFERENCES barber_services(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_appointments_promotion
        FOREIGN KEY (promotion_id) REFERENCES promotions(id)
        ON DELETE SET NULL,

    CONSTRAINT ck_appointments_range CHECK (end_at > start_at),

    INDEX ix_appointments_customer_id (customer_id),
    INDEX ix_appointments_service_id (barber_service_id),
    INDEX ix_appointments_start_at (start_at),
    -- the index the availability overlap query rides on
    INDEX ix_appointments_status_range (status, start_at, end_at),
    INDEX ix_appointments_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
