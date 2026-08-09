using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Reflection;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Imaging;
using System.Windows.Threading;

namespace MeltingFaceRain
{
    public static class Program
    {
        [STAThread]
        public static void Main(string[] args)
        {
            int dropCount = 240;
            for (int i = 0; i < args.Length - 1; i++)
            {
                int parsed;
                if ((args[i].Equals("--count", StringComparison.OrdinalIgnoreCase) ||
                     args[i].Equals("-count", StringComparison.OrdinalIgnoreCase)) &&
                    int.TryParse(args[i + 1], out parsed))
                {
                    dropCount = Math.Max(1, parsed);
                }
            }

            var app = new Application();
            app.Run(new RainWindow(dropCount));
        }
    }

    public sealed class RainWindow : Window
    {
        private readonly Random random = new Random();
        private readonly Grid root = new Grid();
        private readonly Canvas canvas = new Canvas();
        private readonly TextBlock shortcutOverlay = new TextBlock();
        private readonly List<Drop> drops = new List<Drop>();
        private readonly BitmapImage emoji;
        private readonly Stopwatch clock = Stopwatch.StartNew();
        private readonly Stopwatch fpsClock = Stopwatch.StartNew();
        private readonly double baseDropDensity;
        private int frames;

        public RainWindow(int dropCount)
        {
            baseDropDensity = Math.Max(1, dropCount) / (980.0 * 720.0);
            emoji = LoadEmbeddedImage();

            Title = "Melting Face Rain - Default GPU";
            Width = 980;
            Height = 720;
            WindowStartupLocation = WindowStartupLocation.CenterScreen;
            Background = new SolidColorBrush(Color.FromRgb(4, 8, 6));
            Content = root;

            canvas.ClipToBounds = true;
            canvas.Background = Background;
            RenderOptions.SetBitmapScalingMode(canvas, BitmapScalingMode.LowQuality);
            RenderOptions.SetEdgeMode(canvas, EdgeMode.Unspecified);
            root.Children.Add(canvas);

            shortcutOverlay.Text = "F: fullscreen   Q/Esc: quit";
            shortcutOverlay.Foreground = new SolidColorBrush(Color.FromRgb(220, 255, 225));
            shortcutOverlay.Background = new SolidColorBrush(Color.FromArgb(150, 0, 0, 0));
            shortcutOverlay.FontFamily = new FontFamily("Consolas");
            shortcutOverlay.FontSize = 16;
            shortcutOverlay.Padding = new Thickness(12, 7, 12, 7);
            shortcutOverlay.Margin = new Thickness(12);
            shortcutOverlay.HorizontalAlignment = HorizontalAlignment.Left;
            shortcutOverlay.VerticalAlignment = VerticalAlignment.Top;
            shortcutOverlay.IsHitTestVisible = false;
            Panel.SetZIndex(shortcutOverlay, 10000);
            root.Children.Add(shortcutOverlay);

            Loaded += OnLoaded;
            Closed += OnClosed;
            KeyDown += OnKeyDown;
            SizeChanged += OnSizeChanged;
        }

        private static BitmapImage LoadEmbeddedImage()
        {
            var stream = Assembly.GetExecutingAssembly().GetManifestResourceStream("MeltingFace.png");
            if (stream == null)
            {
                throw new InvalidOperationException("Embedded emoji image resource was not found.");
            }

            var bitmap = new BitmapImage();
            bitmap.BeginInit();
            bitmap.CacheOption = BitmapCacheOption.OnLoad;
            bitmap.StreamSource = stream;
            bitmap.EndInit();
            bitmap.Freeze();
            return bitmap;
        }

        private void OnLoaded(object sender, RoutedEventArgs e)
        {
            UpdateDropCountForArea();
            CompositionTarget.Rendering += OnRendering;
            StartShortcutFade();
        }

        private void OnSizeChanged(object sender, SizeChangedEventArgs e)
        {
            UpdateDropCountForArea();
        }

        private void NewDropSprite()
        {
            var image = new Image
            {
                Source = emoji,
                Stretch = Stretch.Fill,
                SnapsToDevicePixels = false
            };

            RenderOptions.SetBitmapScalingMode(image, BitmapScalingMode.LowQuality);

            var rotate = new RotateTransform();
            var translate = new TranslateTransform();
            var transforms = new TransformGroup();
            transforms.Children.Add(rotate);
            transforms.Children.Add(translate);
            image.RenderTransform = transforms;
            image.RenderTransformOrigin = new Point(0.5, 0.5);

            Canvas.SetLeft(image, 0);
            Canvas.SetTop(image, 0);
            canvas.Children.Add(image);

            var drop = new Drop
            {
                Image = image,
                Rotate = rotate,
                Translate = translate
            };

            drops.Add(drop);
            ResetDrop(drop, true);
        }

        private void UpdateDropCountForArea()
        {
            int targetCount = Math.Max(12, (int)Math.Round(StageWidth() * StageHeight() * baseDropDensity));
            while (drops.Count < targetCount)
            {
                NewDropSprite();
            }

            while (drops.Count > targetCount)
            {
                int lastIndex = drops.Count - 1;
                canvas.Children.Remove(drops[lastIndex].Image);
                drops.RemoveAt(lastIndex);
            }
        }

        private void OnClosed(object sender, EventArgs e)
        {
            CompositionTarget.Rendering -= OnRendering;
        }

        private void OnKeyDown(object sender, KeyEventArgs e)
        {
            if (e.Key == Key.Escape || e.Key == Key.Q)
            {
                Close();
            }
            else if (e.Key == Key.F)
            {
                ToggleFullscreen();
            }
        }

        private void ToggleFullscreen()
        {
            if (WindowStyle == WindowStyle.None)
            {
                WindowStyle = WindowStyle.SingleBorderWindow;
                ResizeMode = ResizeMode.CanResize;
                WindowState = WindowState.Normal;
            }
            else
            {
                WindowStyle = WindowStyle.None;
                ResizeMode = ResizeMode.NoResize;
                WindowState = WindowState.Maximized;
            }
        }

        private void StartShortcutFade()
        {
            var timer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(3) };
            timer.Tick += delegate
            {
                timer.Stop();
                var fade = new DoubleAnimation(0, TimeSpan.FromMilliseconds(900));
                shortcutOverlay.BeginAnimation(UIElement.OpacityProperty, fade);
            };
            timer.Start();
        }

        private double StageWidth()
        {
            return Math.Max(1, canvas.ActualWidth);
        }

        private double StageHeight()
        {
            return Math.Max(1, canvas.ActualHeight);
        }

        private void ResetDrop(Drop drop, bool initial)
        {
            double stageWidth = StageWidth();
            double stageHeight = StageHeight();
            double z = random.NextDouble();
            double depth = Math.Pow(z, 1.35);
            double depthScale = 0.23 + (depth * 2.17);

            int size = (int)(Next(42, 86) * depthScale);
            size = Math.Max(12, Math.Min(226, size));

            drop.Size = size;
            drop.X = Next(-size, Math.Max(1, (int)stageWidth));
            drop.Y = -(size + Next(12, Math.Max(24, (int)(stageHeight * (initial ? 2.2 : 0.7)))));
            drop.Speed = (random.NextDouble() * 280 + 180) * (0.32 + depth * 2.55);
            drop.Drift = (random.NextDouble() - 0.5) * (5 + depth * 62);
            drop.Rotation = random.NextDouble() * 360;
            drop.Spin = (random.NextDouble() - 0.5) * (6 + depth * 118);

            drop.Image.Width = size;
            drop.Image.Height = size;
            drop.Image.Opacity = 0.16 + (depth * 0.84);
            drop.Rotate.Angle = drop.Rotation;
            drop.Translate.X = drop.X;
            drop.Translate.Y = drop.Y;
            Panel.SetZIndex(drop.Image, (int)(depth * 1000));
        }

        private int Next(int min, int max)
        {
            return random.Next(min, Math.Max(min + 1, max));
        }

        private void OnRendering(object sender, EventArgs e)
        {
            double elapsed = Math.Min(0.05, clock.Elapsed.TotalSeconds);
            clock.Restart();
            double stageHeight = StageHeight();

            foreach (var drop in drops)
            {
                drop.Y += drop.Speed * elapsed;
                drop.X += drop.Drift * elapsed;
                drop.Rotation += drop.Spin * elapsed;

                if (drop.Y > stageHeight + drop.Size)
                {
                    ResetDrop(drop, false);
                    continue;
                }

                drop.Translate.X = drop.X;
                drop.Translate.Y = drop.Y;
                drop.Rotate.Angle = drop.Rotation;
            }

            frames++;
            if (fpsClock.ElapsedMilliseconds >= 1000)
            {
                int fps = (int)Math.Round(frames * 1000.0 / fpsClock.ElapsedMilliseconds);
                Title = "Melting Face Rain - " + drops.Count + " emojis - " + fps + " FPS - Default GPU";
                frames = 0;
                fpsClock.Restart();
            }
        }
    }

    public sealed class Drop
    {
        public Image Image;
        public RotateTransform Rotate;
        public TranslateTransform Translate;
        public double X;
        public double Y;
        public double Size;
        public double Speed;
        public double Drift;
        public double Rotation;
        public double Spin;
    }
}
