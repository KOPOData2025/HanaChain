"""
Migrate 13-feature DQN model to 17-feature model.

This script creates a new 17-feature model and initializes the weights
from the existing 13-feature model. The new features (14-17) are initialized
with small random values.
"""

import sys
from pathlib import Path

# Add project root to path
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

import numpy as np
import tensorflow as tf
from src.dqn_model import DQNModel
import yaml

def migrate_13_to_17_features(
    old_model_path: str,
    new_model_path: str
):
    """
    Migrate 13-feature model to 17-feature model.

    Args:
        old_model_path: Path to 13-feature model (without extension)
        new_model_path: Path to save 17-feature model (without extension)
    """
    print("=" * 80)
    print("Migrating 13-feature model to 17-feature model")
    print("=" * 80)

    # Load old model configuration
    old_config_path = Path(old_model_path).with_suffix('.config.yaml')
    with open(old_config_path, 'r') as f:
        old_config = yaml.safe_load(f)

    print(f"\n📥 Loading 13-feature model from: {old_model_path}")
    print(f"   Old config: {old_config}")

    # Create old model (13 features)
    old_model = DQNModel(input_dim=13, **{k: v for k, v in old_config.items() if k != 'input_dim'})

    # Load old weights
    old_weights_path = Path(old_model_path).with_suffix('.weights.h5')
    old_model.model.load_weights(str(old_weights_path))
    print(f"✅ Loaded old model weights from: {old_weights_path}")

    # Create new model (17 features)
    print(f"\n🔧 Creating new 17-feature model...")
    new_model = DQNModel(input_dim=17, **{k: v for k, v in old_config.items() if k != 'input_dim'})

    # Get old and new model weights
    old_weights = old_model.model.get_weights()
    new_weights = new_model.model.get_weights()

    print(f"\n📊 Weight transfer:")
    print(f"   Old model layers: {len(old_weights)} weight matrices")
    print(f"   New model layers: {len(new_weights)} weight matrices")

    # Transfer weights layer by layer
    for i, (old_w, new_w) in enumerate(zip(old_weights, new_weights)):
        print(f"\n   Layer {i}:")
        print(f"      Old shape: {old_w.shape}")
        print(f"      New shape: {new_w.shape}")

        if i == 0:  # First layer (input layer)
            # Old: (13, 64), New: (17, 64)
            # Copy first 13 input weights, initialize last 4 with small random values
            if old_w.shape == (13, 64) and new_w.shape == (17, 64):
                new_weights[i][:13, :] = old_w  # Copy first 13 features
                new_weights[i][13:, :] = np.random.randn(4, 64) * 0.01  # Initialize last 4 features
                print(f"      ✅ Transferred first 13 input weights, initialized 4 new features")
            else:
                print(f"      ⚠️  Unexpected shapes, copying as-is")
                new_weights[i] = old_w if old_w.shape == new_w.shape else new_w
        else:
            # Other layers: copy if shapes match
            if old_w.shape == new_w.shape:
                new_weights[i] = old_w
                print(f"      ✅ Copied weights")
            else:
                print(f"      ⚠️  Shape mismatch, keeping new random weights")

    # Set new weights
    new_model.model.set_weights(new_weights)
    print(f"\n✅ Weight transfer complete")

    # Save new model
    print(f"\n💾 Saving new 17-feature model to: {new_model_path}")
    new_model.save_model(str(new_model_path))
    print(f"✅ Model saved successfully")

    # Verify saved model
    verify_path = Path(new_model_path).with_suffix('.config.yaml')
    with open(verify_path, 'r') as f:
        saved_config = yaml.safe_load(f)

    print(f"\n✅ Verification:")
    print(f"   Saved config input_dim: {saved_config['input_dim']}")
    assert saved_config['input_dim'] == 17, "Model saved with wrong input_dim!"

    print("\n" + "=" * 80)
    print("Migration completed successfully!")
    print("=" * 80)


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Migrate 13-feature model to 17-feature model")
    parser.add_argument(
        "--old-model",
        default="data/models/dqn_agent_final_main",
        help="Path to old 13-feature model (without extension)"
    )
    parser.add_argument(
        "--new-model",
        default="data/models/dqn_agent_final_17feat_main",
        help="Path to save new 17-feature model (without extension)"
    )

    args = parser.parse_args()

    migrate_13_to_17_features(args.old_model, args.new_model)
