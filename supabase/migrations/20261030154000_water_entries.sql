-- Migration: water_entries
-- Spec: docs/specs/nutrition-module.md
-- Issue: #96

CREATE TABLE public.water_entries (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES auth.users (id) ON DELETE CASCADE,
  drunk_on date NOT NULL,
  amount_ml numeric NOT NULL
    CONSTRAINT water_entries_amount_check CHECK (amount_ml > 0),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX water_entries_user_drunk_on_idx
  ON public.water_entries (user_id, drunk_on);

CREATE TRIGGER water_entries_set_updated_at
  BEFORE UPDATE ON public.water_entries
  FOR EACH ROW
  EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE public.water_entries ENABLE ROW LEVEL SECURITY;

CREATE POLICY water_entries_select_own ON public.water_entries FOR SELECT TO authenticated
  USING (user_id = auth.uid());
CREATE POLICY water_entries_insert_own ON public.water_entries FOR INSERT TO authenticated
  WITH CHECK (user_id = auth.uid());
CREATE POLICY water_entries_update_own ON public.water_entries FOR UPDATE TO authenticated
  USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());
CREATE POLICY water_entries_delete_own ON public.water_entries FOR DELETE TO authenticated
  USING (user_id = auth.uid());

GRANT SELECT, INSERT, UPDATE, DELETE ON public.water_entries TO authenticated;
