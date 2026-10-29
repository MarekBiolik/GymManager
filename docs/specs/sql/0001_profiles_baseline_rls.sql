-- =============================================================================
-- DRAFT ONLY — NOT APPLIED
-- Spec: docs/specs/first-migration-profiles-rls.md
-- Target path when implementing (#21): supabase/migrations/<timestamp>_profiles_baseline_rls.sql
-- Do NOT run on Cloud `dev` or local from this docs/ tree. Issue #6 = documentation.
-- Blocked apply until: #14 (supabase/) + #21 (copy 1:1 + push).
-- =============================================================================

-- Extensions (Supabase usually has pgcrypto; gen_random_uuid needs pgcrypto or pg 13+)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- -----------------------------------------------------------------------------
-- updated_at helper (LWW — ADR 0008)
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;
$$;

-- -----------------------------------------------------------------------------
-- profiles (minimum PII — ADR 0007)
-- -----------------------------------------------------------------------------
CREATE TABLE public.profiles (
  id uuid PRIMARY KEY REFERENCES auth.users (id) ON DELETE CASCADE,
  preferred_units text NOT NULL DEFAULT 'kg'
    CONSTRAINT profiles_preferred_units_check CHECK (preferred_units IN ('kg', 'lb')),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TRIGGER profiles_set_updated_at
  BEFORE UPDATE ON public.profiles
  FOR EACH ROW
  EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY profiles_select_own
  ON public.profiles
  FOR SELECT
  TO authenticated
  USING (id = auth.uid());

CREATE POLICY profiles_insert_own
  ON public.profiles
  FOR INSERT
  TO authenticated
  WITH CHECK (id = auth.uid());

CREATE POLICY profiles_update_own
  ON public.profiles
  FOR UPDATE
  TO authenticated
  USING (id = auth.uid())
  WITH CHECK (id = auth.uid());

CREATE POLICY profiles_delete_own
  ON public.profiles
  FOR DELETE
  TO authenticated
  USING (id = auth.uid());

GRANT SELECT, INSERT, UPDATE, DELETE ON public.profiles TO authenticated;

-- Auto-create profile on Auth signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  INSERT INTO public.profiles (id) VALUES (NEW.id);
  RETURN NEW;
END;
$$;

CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW
  EXECUTE FUNCTION public.handle_new_user();

-- -----------------------------------------------------------------------------
-- measurements (domain smoke table — glossary Measurement)
-- -----------------------------------------------------------------------------
CREATE TABLE public.measurements (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES auth.users (id) ON DELETE CASCADE,
  metric text NOT NULL,
  value numeric NOT NULL,
  measured_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX measurements_user_measured_at_idx
  ON public.measurements (user_id, measured_at DESC);

CREATE TRIGGER measurements_set_updated_at
  BEFORE UPDATE ON public.measurements
  FOR EACH ROW
  EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE public.measurements ENABLE ROW LEVEL SECURITY;

CREATE POLICY measurements_select_own
  ON public.measurements
  FOR SELECT
  TO authenticated
  USING (user_id = auth.uid());

CREATE POLICY measurements_insert_own
  ON public.measurements
  FOR INSERT
  TO authenticated
  WITH CHECK (user_id = auth.uid());

CREATE POLICY measurements_update_own
  ON public.measurements
  FOR UPDATE
  TO authenticated
  USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());

CREATE POLICY measurements_delete_own
  ON public.measurements
  FOR DELETE
  TO authenticated
  USING (user_id = auth.uid());

GRANT SELECT, INSERT, UPDATE, DELETE ON public.measurements TO authenticated;

-- =============================================================================
-- END DRAFT — NOT APPLIED
-- =============================================================================
