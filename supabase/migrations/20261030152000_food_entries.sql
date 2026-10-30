-- Migration: food_entries (diary)
-- Spec: docs/specs/nutrition-module.md
-- Issue: #110

CREATE TABLE public.food_entries (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES auth.users (id) ON DELETE CASCADE,
  food_id uuid REFERENCES public.foods (id) ON DELETE SET NULL,
  meal_type_id uuid NOT NULL REFERENCES public.meal_types (id) ON DELETE RESTRICT,
  eaten_on date NOT NULL,
  amount numeric NOT NULL
    CONSTRAINT food_entries_amount_check CHECK (amount > 0),
  unit text NOT NULL
    CONSTRAINT food_entries_unit_check CHECK (unit IN ('g', 'serving')),
  food_serving_id uuid REFERENCES public.food_servings (id) ON DELETE SET NULL,
  grams numeric NOT NULL
    CONSTRAINT food_entries_grams_check CHECK (grams > 0),
  protein_g numeric NOT NULL DEFAULT 0,
  carbs_g numeric NOT NULL DEFAULT 0,
  fat_g numeric NOT NULL DEFAULT 0,
  kcal numeric NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX food_entries_user_eaten_on_idx
  ON public.food_entries (user_id, eaten_on);
CREATE INDEX food_entries_user_meal_idx
  ON public.food_entries (user_id, eaten_on, meal_type_id);

CREATE TRIGGER food_entries_set_updated_at
  BEFORE UPDATE ON public.food_entries
  FOR EACH ROW
  EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE public.food_entries ENABLE ROW LEVEL SECURITY;

CREATE POLICY food_entries_select_own ON public.food_entries FOR SELECT TO authenticated
  USING (user_id = auth.uid());
CREATE POLICY food_entries_insert_own ON public.food_entries FOR INSERT TO authenticated
  WITH CHECK (user_id = auth.uid());
CREATE POLICY food_entries_update_own ON public.food_entries FOR UPDATE TO authenticated
  USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());
CREATE POLICY food_entries_delete_own ON public.food_entries FOR DELETE TO authenticated
  USING (user_id = auth.uid());

GRANT SELECT, INSERT, UPDATE, DELETE ON public.food_entries TO authenticated;
