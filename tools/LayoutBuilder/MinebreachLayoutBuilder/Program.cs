using ImageMagick;

string[] files;
string input = args[0];
string output = args[1];

if (Directory.Exists(input))
    files = Directory.GetFiles(input, "*.png");
else 
    files = [input];

if (!Directory.Exists(output)) Directory.CreateDirectory(output);

Dictionary<string, IPixelCollection<byte>> tiles = [];
foreach (string file in Directory.GetFiles("Tiles"))
{
    tiles.Add(Path.GetFileNameWithoutExtension(file), new MagickImage(new FileInfo(file)).GetPixels());
}

foreach (string file in files)
{
    using MagickImage sourceImage = new(new FileInfo(file));
    using IPixelCollection<byte> sourceImagePixels = sourceImage.GetPixels();
    using StreamWriter outputWriter = new(new FileStream($"{output}/{Path.GetFileNameWithoutExtension(file)}.csv", FileMode.Create));
    outputWriter.NewLine = "\n";

    for (int y = 0; y < sourceImage.Height / 3; y++)
    {
        string line = "";
    
        for (int x = 0; x < sourceImage.Width / 3; x++)
        {
            string tile = "-";

            foreach (var kv in tiles)
            {
                bool skip = false;
            
                for (int ty = 0; ty < 3; ty++)
                {
                    for (int tx = 0; tx < 3; tx++)
                    {
                        var sourceImageColor = sourceImagePixels.GetPixel(x * 3 + tx, y * 3 + ty).ToColor()!;
                        var tileColor = kv.Value.GetPixel(tx, ty).ToColor()!;

                        sourceImageColor.A = 255;
                        tileColor.A = 255;

                        if (!sourceImageColor!.FuzzyEquals(tileColor!, new Percentage(0.05f)))
                        {
                            skip = true;
                            break;
                        }
                    }

                    if (skip) break;
                }
            
                if (skip) continue;

                tile = kv.Key;
                break;
            }

            line += $"{tile,2},".Replace(" ", "-");
        }
    
        outputWriter.WriteLine(line.TrimEnd().Trim(','));
    }
    
    Console.WriteLine($"Converted {file} to CSV layout");
}