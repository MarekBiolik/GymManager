type Props = {
  label: string;
};

export function StaleBanner({ label }: Props) {
  return (
    <p className="nutrition-banner" role="status">
      {label}
    </p>
  );
}
