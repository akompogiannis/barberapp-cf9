-- V2__insert_roles_capabilities.sql
-- Seeds the authorization model. Capabilities are the fine-grained unit checked
-- by hasAuthority(...) / @PreAuthorize; roles are just bundles of them.

INSERT INTO roles (name)
VALUES
    ('ADMIN'),      -- the barber (freelancer) who owns the shop
    ('CUSTOMER');   -- someone who books appointments

INSERT INTO capabilities (name, description)
VALUES
    ('MANAGE_SERVICES',         'Create, edit and retire bookable services'),
    ('MANAGE_SCHEDULE',         'Edit weekly working hours and register time off'),
    ('MANAGE_PROMOTIONS',       'Create and edit promotions'),
    ('VIEW_APPOINTMENTS',       'View every appointment in the shop'),
    ('EDIT_APPOINTMENT',        'Confirm, complete or mark any appointment as a no-show'),
    ('CANCEL_APPOINTMENT',      'Cancel any appointment'),
    ('VIEW_AVAILABILITY',       'Query free slots'),
    ('BOOK_APPOINTMENT',        'Book an appointment'),
    ('VIEW_OWN_APPOINTMENTS',   'View only own appointments'),
    ('CANCEL_OWN_APPOINTMENT',  'Cancel only own appointment');

-- ADMIN gets everything.
INSERT INTO roles_capabilities (role_id, capability_id)
SELECT r.id, c.id
FROM roles r
JOIN capabilities c
WHERE r.name = 'ADMIN';

-- CUSTOMER gets the self-service subset.
INSERT INTO roles_capabilities (role_id, capability_id)
SELECT r.id, c.id
FROM roles r
JOIN capabilities c
WHERE r.name = 'CUSTOMER'
  AND c.name IN (
      'VIEW_AVAILABILITY',
      'BOOK_APPOINTMENT',
      'VIEW_OWN_APPOINTMENTS',
      'CANCEL_OWN_APPOINTMENT'
  );
