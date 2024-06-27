import os
from PIL import Image
import numpy as np
import imagehash
import pickle

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

def process_image(image_path):
    """Process the image to use only Game Boy colors."""
    img = Image.open(image_path).convert('RGB')
    img_array = np.array(img)

    # Apply closest color mapping
    for y in range(img_array.shape[0]):
        for x in range(img_array.shape[1]):
            img_array[y, x] = closest_color(img_array[y, x])

    return img_array

def rle_encode(img_array):
    """Compress the image using Run-Length Encoding."""
    pixels = img_array.flatten()
    encoded = []
    prev_pixel = pixels[0]
    count = 1

    for pixel in pixels[1:]:
        if np.array_equal(pixel, prev_pixel):
            count += 1
        else:
            encoded.append((prev_pixel, count))
            prev_pixel = pixel
            count = 1
    encoded.append((prev_pixel, count))
    
    return encoded

def process_and_hash_images(input_dir, output_dir):
    """Process all images in the specified directory, compress, and hash them."""
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    # Load existing hashes if they exist
    hashes_path = os.path.join(output_dir, 'hashes.pkl')
    if os.path.exists(hashes_path):
        with open(hashes_path, 'rb') as f:
            hashes = pickle.load(f)
    else:
        hashes = {}

    file_counter = len(hashes) + 1

    for filename in os.listdir(input_dir):
        if filename.lower().endswith(('.png', '.jpg', '.jpeg', '.bmp', '.gif')):
            input_path = os.path.join(input_dir, filename)
            img_array = process_image(input_path)
            img_hash = imagehash.phash(Image.fromarray(img_array))

            # Check if the hash already exists
            if any(existing_hash == str(img_hash) for existing_hash in hashes.values()):
                print(f"Skipped {filename}, already processed.")
                file_counter += 1
                continue

            encoded_img = rle_encode(img_array)
            output_filename = f"{file_counter:03d}.rle"
            output_path = os.path.join(output_dir, output_filename)
            
            with open(output_path, 'wb') as f:
                pickle.dump((encoded_img, img_array.shape), f)
            
            # Store the hash of the processed image
            hashes[output_filename] = str(img_hash)
            
            print(f"Processed and hashed {filename} -> {output_filename}")
            file_counter += 1
    
    # Save the updated hashes
    with open(hashes_path, 'wb') as f:
        pickle.dump(hashes, f)

def is_duplicate(new_image_path, processed_images_dir, threshold=10):
    """Check if the new image is a duplicate based on the hash."""
    new_image_array = process_image(new_image_path)
    new_image_hash = imagehash.phash(Image.fromarray(new_image_array))

    # Load the hashes of the processed images
    hashes_path = os.path.join(processed_images_dir, 'hashes.pkl')
    if not os.path.exists(hashes_path):
        return False, None

    with open(hashes_path, 'rb') as f:
        hashes = pickle.load(f)

    for filename, img_hash in hashes.items():
        diff = new_image_hash - imagehash.hex_to_hash(img_hash)
        print(f"Comparing with {filename}, Hash difference: {diff}")
        if diff < threshold:
            return True, filename

    return False, None

# Example usage
input_directory = 'processed_images'
output_directory = 'hashed_images'
#process_and_hash_images(input_directory, output_directory)

# Check for duplicates
new_image_path = 'tests/twoNpcFlipped.png'
is_dup, dup_filename = is_duplicate(new_image_path, output_directory)
if is_dup:
    print(f"The image is a duplicate of {dup_filename}.")
else:
    print("The image is new.")

print("Processing and hashing complete.")
