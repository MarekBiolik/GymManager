-- Migration: foods + food_servings
-- Spec: docs/specs/nutrition-module.md
-- Issue: #92

CREATE TABLE public.foods (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES auth.users (id) ON DELETE CASCADE,
  name text NOT NULL,
  protein_g numeric NOT NULL DEFAULT 0
    CONSTRAINT foods_protein_check CHECK (protein_g >= 0),
  carbs_g numeric NOT NULL DEFAULT 0
    CONSTRAINT foods_carbs_check CHECK (carbs_g >= 0),
  fat_g numeric NOT NULL DEFAULT 0
    CONSTRAINT foods_fat_check CHECK (fat_g >= 0),
  default_serving_g numeric
    CONSTRAINT foods_default_serving_check CHECK (default_serving_g IS NULL OR default_serving_g > 0),
  is_recipe boolean NOT NULL DEFAULT false,
  is_favorite boolean NOT NULL DEFAULT false,
  last_used_at timestamptz,
  external_source text,
  external_id text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT foods_name_not_blank CHECK (char_length(trim(name)) > 0)
);

CREATE INDEX foods_user_name_idx ON public.foods (user_id, name);
CREATE INDEX foods_user_last_used_idx ON public.foods (user_id, last_used_at DESC NULLS LAST);
CREATE UNIQUE INDEX foods_user_external_uidx
  ON public.foods (user_id, external_source, external_id)
  WHERE external_source IS NOT NULL AND external_id IS NOT NULL;

CREATE TRIGGER foods_set_updated_at
  BEFORE UPDATE ON public.foods
  FOR EACH ROW
  EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE public.foods ENABLE ROW LEVEL SECURITY;

CREATE POLICY foods_select_own ON public.foods FOR SELECT TO authenticated
  USING (user_id = auth.uid());
CREATE POLICY foods_insert_own ON public.foods FOR INSERT TO authenticated
  WITH CHECK (user_id = auth.uid());
CREATE POLICY foods_update_own ON public.foods FOR UPDATE TO authenticated
  USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());
CREATE POLICY foods_delete_own ON public.foods FOR DELETE TO authenticated
  USING (user_id = auth.uid());

GRANT SELECT, INSERT, UPDATE, DELETE ON public.foods TO authenticated;

CREATE TABLE public.food_servings (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES auth.users (id) ON DELETE CASCADE,
  food_id uuid NOT NULL REFERENCES public.foods (id) ON DELETE CASCADE,
  label text NOT NULL,
  grams numeric NOT NULL
    CONSTRAINT food_servings_grams_check CHECK (grams > 0),
  position integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT food_servings_label_not_blank CHECK (char_length(trim(label)) > 0)
);

CREATE INDEX food_servings_food_position_idx
  ON public.food_servings (food_id, position);

CREATE TRIGGER food_servings_set_updated_at
  BEFORE UPDATE ON public.food_servings
  FOR EACH ROW
  EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE public.food_servings ENABLE ROW LEVEL SECURITY;

CREATE POLICY food_servings_select_own ON public.food_servings FOR SELECT TO authenticated
  USING (user_id = auth.uid());
CREATE POLICY food_servings_insert_own ON public.food_servings FOR INSERT TO authenticated
  WITH CHECK (user_id = auth.uid());
CREATE POLICY food_servings_update_own ON public.food_servings FOR UPDATE TO authenticated
  USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());
CREATE POLICY food_servings_delete_own ON public.food_servings FOR DELETE TO authenticated
  USING (user_id = auth.uid());

GRANT SELECT, INSERT, UPDATE, DELETE ON public.food_servings TO authenticated;
