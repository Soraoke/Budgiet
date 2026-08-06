use std::{fmt::Display, path::PathBuf, todo};
use crate::{Error, PROJECT_ROOT, command};

#[derive(Clone, Copy)]
#[repr(u8)]
pub enum UniFfiTarget {
    Kotlin, Swift
}
impl UniFfiTarget {
    fn targets(self) -> &'static [&'static str] {
        match self {
            Self::Kotlin => &[
                "aarch64-linux-android",
                "armv7-linux-androideabi",
                "x86_64-linux-android",
                "i686-linux-android",
            ],
            Self::Swift => todo!(),
        }
    }
    fn out_dir(self) -> PathBuf {
        match self {
            Self::Kotlin => PROJECT_ROOT.join("android/app/src/main/kotlin"),
            Self::Swift => todo!(),
        }
    }
}
impl Display for UniFfiTarget {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(match self {
            Self::Kotlin => "kotlin",
            Self::Swift => "swift",
        })
    }
}

pub fn pack_rust_lib(target: UniFfiTarget, verbose: bool, dry: bool) -> Result<(), Error> {
    for target in target.targets() {
        command!("cargo", "build", "--package", "budgietlib", "--release", "--target", target)?;
    }
    command!("cargo", "run", "--release", "--bin", "uniffi-bindgen", "--",
        "generate", PROJECT_ROOT.join("target/release/libbudgiet.so"),
        "--config", PROJECT_ROOT.join("uniffi.toml"),
        "--library",
        "--crate", "budgietlib",
        "--language", target.to_string(),
        "--out-dir", target.out_dir(),
        "--no-format"
    )
}
