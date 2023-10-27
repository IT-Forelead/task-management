CREATE TYPE ROLE AS ENUM ('tech_admin', 'junior', 'senior');

CREATE TABLE IF NOT EXISTS users(
  id UUID PRIMARY KEY NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  firstname VARCHAR NOT NULL,
  lastname VARCHAR NOT NULL,
  role ROLE NOT NULL,
  phone VARCHAR NOT NULL UNIQUE,
  password VARCHAR NOT NULL
);

INSERT INTO
  users
VALUES
  (
    '72a911c8-ad24-4e2d-8930-9c3ba51741df',
    '2023-06-30T16:02:51+05:00',
    'Maftunbek',
    'Raxmatov',
    'tech_admin',
    '+998999673398',
    '$s0$e0801$5JK3Ogs35C2h5htbXQoeEQ==$N7HgNieSnOajn1FuEB7l4PhC6puBSq+e1E8WUaSJcGY='
  );
