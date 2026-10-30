-- Migration: recipe_ingredients
-- Spec: docs/specs/nutrition-module.md
-- Issue: #95

CREATE TABLE public.recipe_ingredients (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES auth.users (id) ON DELETE CASCADE,
  recipe_food_id uuid NOT NULL REFERENCES public.foods (id) ON DELETE CASCADE,
  component_food_id uuid NOT NULL REFERENCES public.foods (id) ON DELETE RESTRICT,
  grams numeric NOT NULL
    CONSTRAINT recipe_ingredients_grams_check CHECK (grams > 0),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT recipe_ingredients_not_self CHECK (recipe_food_id <> component_food_id)
);

CREATE INDEX recipe_ingredients_recipe_idx
  ON public.recipe_ingredients (recipe_food_id);

CREATE TRIGGER recipe_ingredients_set_updated_at
  BEFORE UPDATE ON public.recipe_ingredients
  FOR EACH ROW
  EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE public.recipe_ingredients ENABLE ROW LEVEL SECURITY;

CREATE POLICY recipe_ingredients_select_own ON public.recipe_ingredients FOR SELECT TO authenticated
  USING (user_id = auth.uid());
CREATE POLICY recipe_ingredients_insert_own ON public.recipe_ingredients FOR INSERT TO authenticated
  WITH CHECK (user_id = auth.uid());
CREATE POLICY recipe_ingredients_update_own ON public.recipe_ingredients FOR UPDATE TO authenticated
  USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());
CREATE POLICY recipe_ingredients_delete_own ON public.recipe_ingredients FOR DELETE TO authenticated
  USING (user_id = auth.uid());

GRANT SELECT, INSERT, UPDATE, DELETE ON public.recipe_ingredients TO authenticated;
