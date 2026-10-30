-- Migration: nutrition_settings + meal_types
-- Spec: docs/specs/nutrition-module.md
-- Issue: #104

-- -----------------------------------------------------------------------------
-- nutrition_settings (1:1 goals / height / water target)
-- -----------------------------------------------------------------------------
CREATE TABLE public.nutrition_settings (
  user_id uuid PRIMARY KEY REFERENCES auth.users (id) ON DELETE CASCADE,
  calorie_target_kcal numeric NOT NULL DEFAULT 2000
    CONSTRAINT nutrition_settings_calorie_check CHECK (calorie_target_kcal > 0),
  protein_target_g numeric NOT NULL DEFAULT 150
    CONSTRAINT nutrition_settings_protein_check CHECK (protein_target_g >= 0),
  carbs_target_g numeric NOT NULL DEFAULT 200
    CONSTRAINT nutrition_settings_carbs_check CHECK (carbs_target_g >= 0),
  fat_target_g numeric NOT NULL DEFAULT 65
    CONSTRAINT nutrition_settings_fat_check CHECK (fat_target_g >= 0),
  height_cm numeric
    CONSTRAINT nutrition_settings_height_check CHECK (height_cm IS NULL OR height_cm > 0),
  water_target_ml numeric
    CONSTRAINT nutrition_settings_water_check CHECK (water_target_ml IS NULL OR water_target_ml > 0),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TRIGGER nutrition_settings_set_updated_at
  BEFORE UPDATE ON public.nutrition_settings
  FOR EACH ROW
  EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE public.nutrition_settings ENABLE ROW LEVEL SECURITY;

CREATE POLICY nutrition_settings_select_own
  ON public.nutrition_settings FOR SELECT TO authenticated
  USING (user_id = auth.uid());

CREATE POLICY nutrition_settings_insert_own
  ON public.nutrition_settings FOR INSERT TO authenticated
  WITH CHECK (user_id = auth.uid());

CREATE POLICY nutrition_settings_update_own
  ON public.nutrition_settings FOR UPDATE TO authenticated
  USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());

CREATE POLICY nutrition_settings_delete_own
  ON public.nutrition_settings FOR DELETE TO authenticated
  USING (user_id = auth.uid());

GRANT SELECT, INSERT, UPDATE, DELETE ON public.nutrition_settings TO authenticated;

-- -----------------------------------------------------------------------------
-- meal_types (editable diary sections)
-- -----------------------------------------------------------------------------
CREATE TABLE public.meal_types (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES auth.users (id) ON DELETE CASCADE,
  name text NOT NULL,
  position integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT meal_types_name_not_blank CHECK (char_length(trim(name)) > 0)
);

CREATE INDEX meal_types_user_position_idx
  ON public.meal_types (user_id, position);

CREATE TRIGGER meal_types_set_updated_at
  BEFORE UPDATE ON public.meal_types
  FOR EACH ROW
  EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE public.meal_types ENABLE ROW LEVEL SECURITY;

CREATE POLICY meal_types_select_own
  ON public.meal_types FOR SELECT TO authenticated
  USING (user_id = auth.uid());

CREATE POLICY meal_types_insert_own
  ON public.meal_types FOR INSERT TO authenticated
  WITH CHECK (user_id = auth.uid());

CREATE POLICY meal_types_update_own
  ON public.meal_types FOR UPDATE TO authenticated
  USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());

CREATE POLICY meal_types_delete_own
  ON public.meal_types FOR DELETE TO authenticated
  USING (user_id = auth.uid());

GRANT SELECT, INSERT, UPDATE, DELETE ON public.meal_types TO authenticated;

-- -----------------------------------------------------------------------------
-- Seed nutrition_settings + default meal types on new Auth user
-- (extends handle_new_user from baseline migration)
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  INSERT INTO public.profiles (id) VALUES (NEW.id);
  INSERT INTO public.nutrition_settings (user_id) VALUES (NEW.id);
  INSERT INTO public.meal_types (user_id, name, position) VALUES
    (NEW.id, 'Breakfast', 0),
    (NEW.id, '2nd breakfast', 1),
    (NEW.id, 'Lunch', 2),
    (NEW.id, 'Snack', 3),
    (NEW.id, 'Dinner', 4);
  RETURN NEW;
END;
$$;

-- Backfill existing profiles (dev smoke users)
INSERT INTO public.nutrition_settings (user_id)
SELECT id FROM public.profiles
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO public.meal_types (user_id, name, position)
SELECT p.id, v.name, v.position
FROM public.profiles p
CROSS JOIN (
  VALUES
    ('Breakfast', 0),
    ('2nd breakfast', 1),
    ('Lunch', 2),
    ('Snack', 3),
    ('Dinner', 4)
) AS v(name, position)
WHERE NOT EXISTS (
  SELECT 1 FROM public.meal_types mt WHERE mt.user_id = p.id
);
