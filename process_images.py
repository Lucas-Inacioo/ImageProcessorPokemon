import os
from PIL import Image
import numpy as np

# Define Game Boy colors
GAMEBOY_COLORS = [
    (0x00, 0x00, 0x00),  # Darkest gray
    (0x55, 0x55, 0x55),  # Dark gray
    (0xAA, 0xAA, 0xAA),  # Light gray
    (0xFF, 0xFF, 0xFF)   # White
]

def closest_color(pixel):
    """Find the closest Game Boy color to the given pixel."""
    r, g, b = pixel
    color_diffs = []
    for color in GAMEBOY_COLORS:
        cr, cg, cb = color
        color_diff = np.sqrt((r - cr) ** 2 + (g - cg) ** 2 + (b - cb) ** 2)
        color_diffs.append((color_diff, color))
    return min(color_diffs)[1]

def process_image(image_path, output_path):
    """Process the image to use only Game Boy colors."""
    img = Image.open(image_path).convert('RGB')
    img_array = np.array(img)

    # Apply closest color mapping
    for y in range(img_array.shape[0]):
        for x in range(img_array.shape[1]):
            img_array[y, x] = closest_color(img_array[y, x])

    # Save the processed image
    processed_img = Image.fromarray(img_array)
    processed_img.save(output_path)

def process_all_images(input_dir, output_dir):
    """Process all images in the specified directory."""
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    
    file_counter = 1
    for filename in os.listdir(input_dir):
        if filename.lower().endswith(('.png', '.jpg', '.jpeg', '.bmp', '.gif')):
            input_path = os.path.join(input_dir, filename)
            output_filename = f"{file_counter:03d}.png"
            output_path = os.path.join(output_dir, output_filename)
            process_image(input_path, output_path)
            print(f"Processed {filename} -> {output_filename}")
            file_counter += 1

# Example usage
input_directory = 'image_dataset'
output_directory = 'processed_images'
process_all_images(input_directory, output_directory)

print("Processing complete.")
