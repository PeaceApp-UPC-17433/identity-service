CREATE TABLE public.interest_zones (
    id uuid NOT NULL,
    label character varying(255) NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    radius_meters integer NOT NULL,
    user_id uuid NOT NULL
);

CREATE TABLE public.users (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    email character varying(255) NOT NULL,
    email_verified boolean NOT NULL,
    failed_login_attempts integer NOT NULL,
    jurisdiction character varying(255),
    locked boolean NOT NULL,
    password_hash character varying(255) NOT NULL,
    reputation_score integer NOT NULL,
    role character varying(255) NOT NULL,
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['CIUDADANO'::character varying, 'MODERADOR'::character varying, 'INSTITUCIONAL'::character varying, 'ADMINISTRADOR'::character varying])::text[])))
);

ALTER TABLE ONLY public.interest_zones
    ADD CONSTRAINT interest_zones_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.interest_zones
    ADD CONSTRAINT fk3dcy6mfs8a0x78dj0qt0k099l FOREIGN KEY (user_id) REFERENCES public.users(id);
