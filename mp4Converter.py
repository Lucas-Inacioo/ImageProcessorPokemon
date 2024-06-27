import cv2
import os
from tqdm import tqdm

def video_to_png(video_path, output_folder, save_fps, crop, start_time, end_time):
    """
    Converts a video to a series of PNG images.
    """
    if not os.path.exists(output_folder):
        print(f"Create folder: {output_folder}.")
        try:
            os.makedirs(output_folder, exist_ok=True)
        except Exception as e:
            print(f"Unable to create folder {output_folder}, raise exception {e}")
            return -1

    cap = cv2.VideoCapture(video_path)

    if not cap.isOpened():
        print("Error: Could not open video.")
        return

    video_fps = cap.get(cv2.CAP_PROP_FPS)
    frame_interval = 1 if save_fps == -1 else max(int(round(video_fps / save_fps)), 1)

    start_frame = int(start_time * video_fps)
    end_frame = int(end_time * video_fps)

    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    saved_frame_count = 0

    if end_frame > total_frames:
        end_frame = total_frames

    cap.set(cv2.CAP_PROP_POS_FRAMES, start_frame)

    for frame_id in tqdm(range(start_frame, end_frame), desc="Converting video"):
        ret, frame = cap.read()
        if not ret:
            break
        if crop != ((-1, -1), (-1, -1)):
            left, upper = crop[0]
            right, lower = crop[1]
            frame = frame[upper:lower, left:right]

        if frame_id % frame_interval == 0:
            frame_path = os.path.join(output_folder, f"frame_{saved_frame_count:04d}.png")

            is_success, im_buf_arr = cv2.imencode(".png", frame)
            im_buf_arr.tofile(frame_path)

            saved_frame_count += 1

    cap.release()
    print(f"Done! Extracted {saved_frame_count} frames.")


def main():
    """
    Main function to set paths and call video processing function.
    """
    # Set relative paths and parameters
    input_video_path = "./pokemon.mp4"
    output_folder = "./output"
    save_fps = 5  # Set the desired FPS for saving frames, -1 for original FPS
    crop = ((-1, -1), (-1, -1))  # Set the crop area as a tuple of two points
    start_time = 36
    end_time = 66

    # Process the video
    video_to_png(input_video_path, output_folder, save_fps, crop, start_time, end_time)


if __name__ == "__main__":
    main()
